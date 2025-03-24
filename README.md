# Temporal Projects samples

**Technologies Used**:
- **Language**: Java
- **Build Tool**: Maven
- **Framework**: Temporal
- **Infrastructure**: Docker compose

This project contains two sample applications using Temporal to orchestrate workflows:
- one is a banking application
- the other is an e-commerce order processing application

Please note that this applications do over simplify functional features that real application should provide. There are only a pretext to demonstrate Temporal.io features.

The project also includes configurations to run temporal in a Docker environment. (see `docker-compose.yml` in the src/main/docker directory).

## Running temporal server
To run the temporal server, you can use the provided docker-compose configuration:
```shell
cd src/main/docker
docker-compose up
```
- The Temporal server will start on `localhost:7233`.
- The Web UI will be available at [http://localhost:8088](http://localhost:8088). You can use the Web UI to monitor the workflows and activities.


## Banking Application
1. **Workflows and Activities**:
    - `BankingWorkflowImpl`: Implementation of the banking workflow, managing deposit, withdrawal, and balance checking.
    - `BankingActivitiesImpl`: Implementation of the activities associated with the banking workflow.

2. **Workers**: 
    - `BankTransferWorker`: Starts a Temporal worker to execute banking workflows and activities.

3. **Starter**: 
    - `BankTransferStarter`: Starts a banking workflow with specific parameters (account ID, initial balance, deposit amount, withdrawal amount).

4. **workflow description**

   The workflow manages the transfer of money between two bank accounts. Here are the main steps of the workflow:  
   **Debit the Account**: Debits the specified amount from the source account.
    If the activity fails (insufficient funds), the workflow stops.

   **Credit the Account**: Credits the specified amount to the destination account.
   If the activity fails (e.g., account blocked), handles the error and refunds the source account.

    **Error Handling**:
   - If funds are insufficient, logs the error and throws an exception.
   - If the account is blocked, logs the error, refunds the source account, and throws an exception. 
   - The workflow uses retry options for the activities, with maximum attempts and backoff intervals configured.

### Running the Banking Application
1. **Ensure temporal server is started**:
2. **Start the Worker**: 
    - Run the `BankTransferWorker` class to start a worker that executes banking workflows and activities.
3. **Start the Starter**: 
    - Run the `BankTransferStarter` class to start a banking workflow.
    - The workflow will execute the steps of the banking workflow.

## E-commerce Order Processing
1. **Workflows and Activities**:
    - `OrderWorkflowImpl`: Implementation of the order workflow, managing stock reservation, payment, stock checking, shipping, and cancellations.
    - `OrderActivitiesImpl`: Implementation of the activities associated with the order workflow.

2. **Workers**:
    - `ECommerceWorker`: Starts a Temporal worker to execute order workflows and activities.

3. **Starter**:
    - `ECommerceStarter`: Starts an order workflow with specific parameters (order ID, product ID, quantity).

4. **Stock Notification**:
    - `StockAvailableNotifier`: Notifies the running workflow that stock is available by searching for running `OrderWorkflow` instances. 
    - This code also shows how to search for workflow instances: 

### Running the E-commerce Application
1. **Ensure temporal server is started**:
2. **Start the Worker**:
    - Run the `ECommerceWorker` class to start a worker that executes order workflows and activities.
3. **Start the Starter**: 
    - Run the `ECommerceStarter` class to start an order workflow.
    - The workflow will execute the steps of the order processing workflow.
    - You can also, if necessary, run the `StockAvailableNotifier` to notify the workflow that stock is available.

## Testing the Applications
- Run the tests in the `src/test` directory to test the workflows.