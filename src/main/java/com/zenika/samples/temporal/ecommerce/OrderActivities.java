package com.zenika.samples.temporal.ecommerce;

import io.temporal.activity.ActivityInterface;
import io.temporal.failure.ApplicationFailure;

import java.time.Duration;

@ActivityInterface
public interface OrderActivities {
    String PAYMENT_FAILED = "PAYMENT_FAILED";
    String DELIVERY_FAILED = "DELIVERY_FAILED";

    void reserveStock(String orderId, String productId, int quantity);
    void cancelStockReservation(String orderId, String productId, int quantity);
    void processPayment(String orderId);

    static ApplicationFailure paymentFailure() {
        return ApplicationFailure.newNonRetryableFailure("Payment failed", PAYMENT_FAILED);
    }

    void refundPayment(String orderId);
    boolean checkStock(String orderId, String productId);
    boolean askUserForApproval(String orderId, String productId);
    void shipOrder(String orderId);

    static ApplicationFailure orderDeliveryFailure() {
        return ApplicationFailure.newNonRetryableFailure("Delivery failed", DELIVERY_FAILED);
    }

    void cancelOrder(String orderId);

    default Duration getWaitForStockAvailableTimeout(){
        return Duration.ofMinutes(30);
    }
}