package com.zenika.samples.temporal.ecommerce;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class ECommerceStarter {

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        // Démarrage du workflow
        OrderWorkflow workflow = client.newWorkflowStub(
            OrderWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue("ORDER_TASK_QUEUE").build()
        );

        String result = workflow.processOrder("12345", "PROD-001", 2);
        System.out.println("workflow result : " + result);
    }
}
