package com.zenika.samples.temporal.ecommerce;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.filter.v1.WorkflowTypeFilter;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.ListOpenWorkflowExecutionsRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class StockAvailableNotifier {
    private static final Logger log = LoggerFactory.getLogger(StockAvailableNotifier.class);
    private static WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    private static WorkflowClient client = WorkflowClient.newInstance(service);

    public static void main(String[] args) {
        getFirstRunningWorkflowId()
            .ifPresentOrElse(workflowId -> {
                    log.info("Workflow en cours trouvé : {}", workflowId);
                    OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, workflowId);
                    workflow.notifyStockAvailable();
                },
                () -> log.info("Aucun workflow en cours trouvé !"));
    }


    private static Optional<String> getFirstRunningWorkflowId() {
        // Récupération du workflow en attente = le premier workflow de type "OrderWorkflow" en cours d'exécution
        return service.blockingStub()
            .listOpenWorkflowExecutions(ofTypeOrderWorkflow)
            .getExecutionsList().stream()
            .filter(StockAvailableNotifier::workflowIsRunning)
            .map(WorkflowExecutionInfo::getExecution)
            .map(WorkflowExecution::getWorkflowId)
            .findFirst();
    }

    private static ListOpenWorkflowExecutionsRequest ofTypeOrderWorkflow =
        ListOpenWorkflowExecutionsRequest.newBuilder()
            .setNamespace("default")
            .setTypeFilter(WorkflowTypeFilter.newBuilder().setName("OrderWorkflow").build())
            .build();


    private static boolean workflowIsRunning(WorkflowExecutionInfo workflowExecutionInfo) {
        return workflowExecutionInfo.getStatus() == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING;
    }
}
