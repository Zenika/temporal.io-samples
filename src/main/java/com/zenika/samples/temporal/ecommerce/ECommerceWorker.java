package com.zenika.samples.temporal.ecommerce;

import com.zenika.samples.temporal.ecommerce.implementations.OrderActivitiesImpl;
import com.zenika.samples.temporal.ecommerce.implementations.OrderWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class ECommerceWorker {

    public static final String ORDER_TASK_QUEUE = "ORDER_TASK_QUEUE";

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(ORDER_TASK_QUEUE);

        worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);
        worker.registerActivitiesImplementations(new OrderActivitiesImpl());

        factory.start();
        System.out.println("🚀 Temporal Worker started...");
    }
}
