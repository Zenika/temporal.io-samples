package com.zenika.samples.temporal.bank;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class BankTransferStarter {
    private static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    private static final WorkflowClient client = WorkflowClient.newInstance(service);

    private static final Logger LOGGER = getLogger(BankTransferStarter.class);

    public static void main(String[] args) {

        transfer("C789", "A123", 100.0); // should fail
        transfer("A123", "C789", 100.0); // should succeed
        transfer("A123", "C789", 100.0);// should succeed

    }

    private static void transfer(String fromAccount, String toAccount, double amount) {
        BankTransferWorkflow workflow = client.newWorkflowStub(
            BankTransferWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("BANK_TRANSFER_TASK_QUEUE")
                .build()
        );

        try {
            workflow.transfer(new TransactionRequirement(fromAccount, toAccount, amount));
            LOGGER.info("✅ Workflow succeeded");
        } catch (Exception e) {
            LOGGER.error("❌ Workflow failed", e);
        }
    }
}