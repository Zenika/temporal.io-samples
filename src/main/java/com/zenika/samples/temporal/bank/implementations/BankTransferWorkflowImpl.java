package com.zenika.samples.temporal.bank.implementations;

import com.zenika.samples.temporal.Exceptions;
import com.zenika.samples.temporal.bank.BankAccountActivities;
import com.zenika.samples.temporal.bank.BankTransferWorkflow;
import com.zenika.samples.temporal.bank.TransactionRequirement;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class BankTransferWorkflowImpl implements BankTransferWorkflow {

    private final BankAccountActivities activities = Workflow.newActivityStub(
        BankAccountActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofHours(5))
            .setRetryOptions(RetryOptions.newBuilder()
                .setDoNotRetry(BankAccountActivities.ACCOUNT_BLOCKED, BankAccountActivities.INSUFFICIENT_FUNDS)
                .setMaximumAttempts(5)
                .setInitialInterval(Duration.ofSeconds(1))
                .setBackoffCoefficient(1.5)
                .build())
            .build()
    );

    private static final Logger LOGGER = Workflow.getLogger(BankTransferWorkflowImpl.class);
    @Override
    public void transfer(TransactionRequirement transactionRequirement) {
        try {
            activities.debitAccount(transactionRequirement.fromAccount(), transactionRequirement.amount());
            LOGGER.info("💸 Debit: {} from account {}", transactionRequirement.amount(), transactionRequirement.fromAccount());
            try {
                activities.creditAccount(transactionRequirement.toAccount(), transactionRequirement.amount());
                LOGGER.info("💰 Credit: {} to account {}", transactionRequirement.amount(), transactionRequirement.toAccount());
            } catch (ActivityFailure e) {
                handleBlockedAccount(transactionRequirement, e);
            }
        } catch (ActivityFailure e) {
            handleInsufficientFunds(e);
        }
        LOGGER.info("✅ Transfer succeeded");
    }

    private void handleBlockedAccount(TransactionRequirement transactionRequirement, ActivityFailure e) {
        if (Exceptions.hasCause(e, BankAccountActivities.ACCOUNT_BLOCKED)) {
            LOGGER.error("⚠️ ERROR - Account blocked", e);
            activities.refundAccount(transactionRequirement.fromAccount(), transactionRequirement.amount());
            throw e;
        }
        throw e;
    }

    private static void handleInsufficientFunds(ActivityFailure e) {
        if (Exceptions.hasCause(e, BankAccountActivities.INSUFFICIENT_FUNDS)) {
            LOGGER.error("🚨 ERROR - Insufficient funds", e);
            throw e;
        }
        throw e;
    }
}