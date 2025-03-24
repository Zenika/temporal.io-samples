package com.zenika.samples.temporal.bank;

import com.zenika.samples.temporal.bank.implementations.BankAccountActivitiesImpl;
import com.zenika.samples.temporal.bank.implementations.BankTransferWorkflowImpl;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.zenika.samples.temporal.bank.BankTransferWorker.BANK_TRANSFER_TASK_QUEUE;
import static org.assertj.core.api.Assertions.assertThat;

class BankTransferWorkflowTest {

    private BankTransferWorkflow bankTransferWorkflow;
    private BankAccountActivitiesImpl bankAccountActivities;
    private TestWorkflowEnvironment testEnv;

    // Set up the test workflow environment
    @BeforeEach
    public void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        var worker = testEnv.newWorker(BANK_TRANSFER_TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(BankTransferWorkflowImpl.class);
        bankAccountActivities = new BankAccountActivitiesImpl();
        worker.registerActivitiesImplementations(bankAccountActivities);

        var client = testEnv.getWorkflowClient();
        // Start test environment
        testEnv.start();

        bankTransferWorkflow = client.newWorkflowStub(
            BankTransferWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(BANK_TRANSFER_TASK_QUEUE).build()
        );
    }

    // Clean up test environment after tests are completed
    @AfterEach
    public void tearDown() {
        testEnv.close();
    }

    @Test
    @DisplayName("Transfer should succeed")
    void testTransferSuccess() {
        // Given
        var fromAccount = "A123";
        var toAccount = "C789";
        var fromBalance = bankAccountActivities.getBalance(fromAccount);
        var toBalance = bankAccountActivities.getBalance(toAccount);
        var amount = 100;
        TransactionRequirement transactionRequirement = new TransactionRequirement(fromAccount, toAccount, amount);
        // When
        bankTransferWorkflow.transfer(transactionRequirement);
        // Then
        var newFromBalance = bankAccountActivities.getBalance(fromAccount);
        var newToBalance = bankAccountActivities.getBalance(toAccount);
        assertThat(newFromBalance).isEqualTo(fromBalance - amount);
        assertThat(newToBalance).isEqualTo(toBalance + amount);
    }

    @Test
    @DisplayName("Transfer should fail because the source account has insufficient funds")
    void testTransferFailInsufficientFunds() {
        // Given
        var fromAccount = "C789";
        var toAccount = "A123";
        var fromBalance = bankAccountActivities.getBalance(fromAccount);
        var toBalance = bankAccountActivities.getBalance(toAccount);
        TransactionRequirement transactionRequirement = new TransactionRequirement(fromAccount, toAccount, 100);
        // When / Then
        Assertions.assertThatThrownBy(() -> bankTransferWorkflow.transfer(transactionRequirement))
            .isInstanceOf(WorkflowFailedException.class)
            .cause().isInstanceOf(ActivityFailure.class)
            .cause().hasMessageContaining("Insufficient funds in account C789");
        assertThat(bankAccountActivities.getBalance(fromAccount)).isEqualTo(fromBalance);
        assertThat(bankAccountActivities.getBalance(toAccount)).isEqualTo(toBalance);
    }

    @Test
    @DisplayName("Transfer should fail because the target account is blocked")
    void testTransferFailAccountBlocked() {
        // Given
        var fromAccount = "A123";
        var toAccount = "B456";
        var fromBalance = bankAccountActivities.getBalance(fromAccount);
        var toBalance = bankAccountActivities.getBalance(toAccount);
        TransactionRequirement transactionRequirement = new TransactionRequirement(fromAccount, toAccount, 100);

        // When / Then
        Assertions.assertThatThrownBy(() -> bankTransferWorkflow.transfer(transactionRequirement))
            .isInstanceOf(WorkflowFailedException.class)
            .cause().isInstanceOf(ActivityFailure.class)
            .cause().hasMessageContaining("Account B456 is blocked!");
        assertThat(bankAccountActivities.getBalance(fromAccount)).isEqualTo(fromBalance);
        assertThat(bankAccountActivities.getBalance(toAccount)).isEqualTo(toBalance);
    }
}