package com.zenika.samples.temporal.bank;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface BankTransferWorkflow {
    @WorkflowMethod
    void transfer(TransactionRequirement transactionRequirement);
}