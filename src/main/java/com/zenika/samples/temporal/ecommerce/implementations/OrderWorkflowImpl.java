package com.zenika.samples.temporal.ecommerce.implementations;

import com.zenika.samples.temporal.Exceptions;
import com.zenika.samples.temporal.ecommerce.OrderActivities;
import com.zenika.samples.temporal.ecommerce.OrderWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class OrderWorkflowImpl implements OrderWorkflow {

    private final ActivityOptions options = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(10))
        .build();

    private final OrderActivities activities = Workflow.newActivityStub(OrderActivities.class, options);

    private static final Logger LOGGER = Workflow.getLogger(OrderWorkflowImpl.class);
    private boolean stockAvailable = false;

    @Override
    public String processOrder(String orderId, String productId, int quantity) {
        var paymentSucceeded = false;
        try {
            // Step 1: Reserve the product in stock
            activities.reserveStock(orderId, productId, quantity);

            // Step 2: Call the payment service
            activities.processPayment(orderId);
            paymentSucceeded = true;

            // Step 3: Check if the stock is still available
            boolean isStockAvailable = activities.checkStock(orderId, productId);
            if (!isStockAvailable) {
                boolean userApproval = activities.askUserForApproval(orderId, productId);
                if (!userApproval) {
                    activities.cancelOrder(orderId);
                    activities.refundPayment(orderId);
                    activities.cancelStockReservation(orderId, productId, quantity);
                    return "Insufficient stock. Order canceled by the user.";
                }
                // Wait for restocking
                LOGGER.info("Out of stock for product {}. Waiting for restocking", productId);
                Workflow.await(activities.getWaitForStockAvailableTimeout(), () -> this.stockAvailable);
                LOGGER.info("End of waiting. Stock replenished?: {}", this.stockAvailable);
                if (!this.stockAvailable) {
                    activities.cancelOrder(orderId);
                    activities.refundPayment(orderId);
                    activities.cancelStockReservation(orderId, productId, quantity);
                    return "Order canceled due to lack of restocking.";
                }
            }

            // Step 4: Ship the order
            activities.shipOrder(orderId);
            return "Order successfully shipped.";
        } catch (ActivityFailure e) {
            activities.cancelOrder(orderId);
            if (paymentSucceeded) {
                activities.refundPayment(orderId);
            }
            if (Exceptions.hasCause(e, OrderActivities.PAYMENT_FAILED)) {
                return "Order failed: Payment declined.";
            } else if (Exceptions.hasCause(e, OrderActivities.DELIVERY_FAILED)) {
                return "Order canceled: Delivery failed.";
            }
            return "Order canceled due to an error.";
        }
    }

    // Signal to notify that stock is available
    @Override
    public void notifyStockAvailable() {
        LOGGER.info("Stock replenished. Workflow notification.");
        this.stockAvailable = true;
    }
}