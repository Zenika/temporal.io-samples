package com.zenika.samples.temporal.ecommerce.implementations;

import com.zenika.samples.temporal.ecommerce.OrderActivities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderActivitiesImpl implements OrderActivities {

    private static final Logger log = LoggerFactory.getLogger(OrderActivitiesImpl.class);

    @Override
    public void reserveStock(String orderId, String productId, int quantity) {
        log.info("Stock reserved for order {}", orderId);
    }

    @Override
    public void cancelStockReservation(String orderId, String productId, int quantity) {
        log.info("Stock reservation canceled for order {}", orderId);
    }

    @Override
    public void processPayment(String orderId) {
        if (Math.random() < 0.2) {
            log.error("Payment failed for order {}", orderId);
            throw OrderActivities.paymentFailure();
        }
        log.info("Payment processed for order {}", orderId);
    }

    @Override
    public void refundPayment(String orderId) {
        log.info("Payment refunded for order {}", orderId);
    }

    @Override
    public boolean checkStock(String orderId, String productId) {
        var result = Math.random() > 0.3; // Simule une indisponibilité aléatoire du stock
        if (!result) {
            log.info("Out of stock for product {}", productId);
        }
        return result;
    }

    @Override
    public boolean askUserForApproval(String orderId, String productId) {
        log.info("User must approve the order despite low stock.");
        return true; // Simulation d'une réponse utilisateur positive
    }

    @Override
    public void shipOrder(String orderId) {
        if (Math.random() < 0.2) {
            log.error("Delivery failed for order {}", orderId);
            throw OrderActivities.orderDeliveryFailure();
        }
        log.info("Order shipped!");
    }

    @Override
    public void cancelOrder(String orderId) {
        log.info("Order {} canceled.", orderId);
    }
}