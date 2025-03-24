package com.zenika.samples.temporal.ecommerce;

import com.zenika.samples.temporal.ecommerce.implementations.OrderActivitiesImpl;
import com.zenika.samples.temporal.ecommerce.implementations.OrderWorkflowImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.zenika.samples.temporal.ecommerce.ECommerceWorker.ORDER_TASK_QUEUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OrderWorkflowTest {
    private final String orderId = "ORDER-123";
    private final String productId = "PRODUCT-456";
    private final int quantity = 1;
    private OrderActivities orderActivities;
    private OrderWorkflow orderWorkflow;

    @BeforeEach
    public void setUp() {
        // set useTimeskipping is set to false to make sure that the test runs in real time
        // (i.e. the test will wait for the actual time to pass. This is useful when testing time-based workflows)
        // If useTimeskipping is set to true, the test will run as fast as possible
        var testEnvironmentOptions = TestEnvironmentOptions.newBuilder().setUseTimeskipping(false).build();
        var testEnv = TestWorkflowEnvironment.newInstance(testEnvironmentOptions);
        var worker = testEnv.newWorker(ORDER_TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);
        orderActivities = mock(OrderActivitiesImpl.class);
        worker.registerActivitiesImplementations(orderActivities);
        var client = testEnv.getWorkflowClient();
        orderWorkflow = client.newWorkflowStub(
            OrderWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(ORDER_TASK_QUEUE).build()
        );
        testEnv.start();
    }

    @Test
    @DisplayName("Standard case: Process order should succeed")
    void testProcessOrder() {
        // Given
        stockWillBeAvailable();
        stockReservationWillSucceed();
        paymentWillSucceed();
        shipmentWillSucceed();
        // When
        assertThat(getWorkflowResult()).isEqualTo("Order successfully shipped.");
        // Then
        verifyThatStockHasBeenReserved();
        verifyThatOrderHasBeenPayed();
        verifyThatOrderHasBeenShipped();
        verifyThatPaymentHasNotBeenRefund();
    }

    @Nested
    @DisplayName("Exceptional cases")
    class ExceptionalCases {
        @Test
        @DisplayName("When stock is not available and customer does not wait order should fail")
        void testProcessOrderWhenStockNotAvailable() {
            // Given
            paymentWillSucceed();
            stockWillNotBeAvailable();
            customerWillNotWait();
            // When
            var response = getWorkflowResult();
            // Then
            assertThat(response).isEqualTo("Insufficient stock. Order canceled by the user.");
            verifyThatStockHasBeenReserved();
            verifyThatPaymentHasBeenRefund();
            verifyThatOrderHasBeenCanceled();
        }


        @Test
        @DisplayName("When stock is not available and customer wait and stock is not refilled before timeout order should fail")
        void testProcessOrderWhenStockNotAvailableAndCustomerWaitsAndStockIsNotRefilled() {
            // Given
            paymentWillSucceed();
            stockWillNotBeAvailable();
            customerWillWait();
            stockReservationWillSucceed();
            stockIsNotGoingToBeAvailable();
            // When
            var response = getWorkflowResult();
            // Then
            assertThat(response).isEqualTo("Order canceled due to lack of restocking.");
            verifyThatStockHasBeenReserved();
            verifyThatOrderHasBeenPayed();
            verifyThatStockReservationHasBeenCanceled();
            verifyThatPaymentHasBeenRefund();
        }

        @Test
        @DisplayName("When stock is not available and customer wait and stock will be refilled")
        void testProcessOrderWhenStockNotAvailableAndCustomerWaits() {
            // Given
            stockWillNotBeAvailable();
            customerWillWait();
            stockReservationWillSucceed();
            paymentWillSucceed();
            shipmentWillSucceed();
            stockIsGoingToBeAvailable();
            // When
            var response = getWorkflowResult();
            // Then
            assertThat(response).isEqualTo("Order successfully shipped.");
            verifyThatOrderHasBeenPayed();
            verifyThatStockHasBeenReserved();
            verifyThatStockReservationHasNotBeenCanceled();
            verifyThatOrderHasBeenShipped();
            verifyThatPaymentHasNotBeenRefund();
        }

        @Test
        @DisplayName("When payment fails, order should fail")
        void testProcessOrderWhenPaymentFails() {
            // Given
            stockWillBeAvailable();
            stockReservationWillSucceed();
            paymentWillFail();
            // When
            var response = getWorkflowResult();
            // Then
            assertThat(response).isEqualTo("Order failed: Payment declined.");
            verifyThatPaymentHasNotBeenRefund();
            verifyThatOrderHasNotBeenShipped();
        }

        @Test
        @DisplayName("When shipping fails, order should fail")
        void testProcessOrderWhenShippingFails() {
            // Given
            stockWillBeAvailable();
            stockReservationWillSucceed();
            paymentWillSucceed();
            deliveryWillFail();
            // When
            var response = getWorkflowResult();
            // Then
            assertThat(response).isEqualTo("Order canceled: Delivery failed.");
            verifyThatStockHasBeenReserved();
            verifyThatOrderHasBeenPayed();
            verifyThatPaymentHasBeenRefund();
        }
    }

    private void stockIsGoingToBeAvailable() {
        var waitTimeout = 2000;
        when(orderActivities.getWaitForStockAvailableTimeout()).thenReturn(Duration.ofMillis(waitTimeout));
        new Thread(() -> {
            try {
                Thread.sleep(waitTimeout / 2);
                orderWorkflow.notifyStockAvailable();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void stockIsNotGoingToBeAvailable() {
        var waitTimeout = 100;
        when(orderActivities.getWaitForStockAvailableTimeout()).thenReturn(Duration.ofMillis(waitTimeout));
    }

    private void customerWillWait() {
        when(orderActivities.askUserForApproval(anyString(), anyString()))
            .thenReturn(true);
    }

    private void customerWillNotWait() {
        when(orderActivities.askUserForApproval(anyString(), anyString()))
            .thenReturn(false);
    }

    private void stockWillNotBeAvailable() {
        when(orderActivities.checkStock(anyString(), anyString()))
            .thenReturn(false);
    }

    private void shipmentWillSucceed() {
        doNothing().when(orderActivities).shipOrder(anyString());
    }

    private void paymentWillFail() {
        doThrow(OrderActivities.paymentFailure())
            .when(orderActivities).processPayment(anyString());
    }

    private void deliveryWillFail() {
        doThrow(OrderActivities.orderDeliveryFailure())
            .when(orderActivities).shipOrder(anyString());
    }

    private void stockReservationWillSucceed() {
        doCallRealMethod().when(orderActivities).reserveStock(anyString(), anyString(), anyInt());
    }

    private void paymentWillSucceed() {
        doNothing().when(orderActivities).processPayment(anyString());
    }

    private void stockWillBeAvailable() {
        when(orderActivities.checkStock(anyString(), anyString()))
            .thenReturn(true);
    }

    private String getWorkflowResult() {
        return orderWorkflow.processOrder(orderId, productId, quantity);
    }

    private void verifyThatOrderHasBeenPayed() {
        verify(orderActivities, atLeastOnce()).processPayment(eq(orderId));
    }

    private void verifyThatOrderHasBeenShipped() {
        verify(orderActivities, atLeastOnce()).shipOrder(eq(orderId));
    }

    private void verifyThatOrderHasNotBeenShipped() {
        verify(orderActivities, never()).shipOrder(eq(orderId));
    }

    private void verifyThatPaymentHasNotBeenRefund() {
        verify(orderActivities, never()).refundPayment(eq(orderId));
    }

    private void verifyThatStockHasBeenReserved() {
        verify(orderActivities, atLeastOnce()).reserveStock(eq(orderId), eq(productId), eq(quantity));
    }

    private void verifyThatOrderHasBeenCanceled() {
        verify(orderActivities, atLeastOnce()).cancelOrder(anyString());
    }

    private void verifyThatPaymentHasBeenRefund() {
        verify(orderActivities, atLeastOnce()).refundPayment(eq(orderId));
    }

    private void verifyThatStockReservationHasBeenCanceled() {
        verify(orderActivities, atLeastOnce()).cancelStockReservation(eq(orderId), eq(productId), eq(quantity));
    }

    private void verifyThatStockReservationHasNotBeenCanceled() {
        verify(orderActivities, never()).cancelStockReservation(eq(orderId), eq(productId), eq(quantity));
    }

}