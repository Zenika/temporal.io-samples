package com.zenika.samples.temporal.bank;

import com.zenika.samples.temporal.bank.implementations.BankAccountActivitiesImpl;
import com.zenika.samples.temporal.bank.implementations.BankTransferWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class BankTransferWorker {

    public static final String BANK_TRANSFER_TASK_QUEUE = "BANK_TRANSFER_TASK_QUEUE";

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(BANK_TRANSFER_TASK_QUEUE);

        worker.registerWorkflowImplementationTypes(BankTransferWorkflowImpl.class);
        worker.registerActivitiesImplementations(new BankAccountActivitiesImpl());

        factory.start();
        System.out.println("🚀 Temporal Worker started...");
    }
}
