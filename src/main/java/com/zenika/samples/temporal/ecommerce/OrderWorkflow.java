package com.zenika.samples.temporal.ecommerce;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderWorkflow {

    @WorkflowMethod
    String processOrder(String orderId, String productId, int quantity);

    @SignalMethod
    void notifyStockAvailable();
}