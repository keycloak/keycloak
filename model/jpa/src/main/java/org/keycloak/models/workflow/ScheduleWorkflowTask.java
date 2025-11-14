package org.keycloak.models.workflow;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.jboss.logging.Logger;

final class ScheduleWorkflowTask extends WorkflowTransactionalTask {

    private static final Logger log = Logger.getLogger(ScheduleWorkflowTask.class);

    private final DefaultWorkflowExecutionContext workflowContext;

    ScheduleWorkflowTask(DefaultWorkflowExecutionContext context) {
        super(context.getSession());
        this.workflowContext = context;
    }

    @Override
    public void run(KeycloakSession session) {
        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        if (realm == null) {
            // during startup realms might be created/imported - skip processing in this case
            return;
        }


        Workflow workflow = workflowContext.getWorkflow();
        WorkflowEvent event = workflowContext.getEvent();
        WorkflowStep firstStep = workflow.getSteps().findFirst().orElseThrow(() -> new WorkflowInvalidStateException("No steps found for workflow " + workflow.getName()));
        log.debugf("Scheduling first step '%s' of workflow '%s' for resource %s based on on event %s with notBefore %d",
                firstStep.getProviderId(), workflow.getName(), event.getResourceId(), event.getOperation(), workflow.getNotBefore());
        String originalAfter = firstStep.getAfter();
        try {
            firstStep.setAfter(workflow.getNotBefore());
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            stateProvider.scheduleStep(workflow, firstStep, event.getResourceId(), workflowContext.getExecutionId());
        } finally {
            // restore the original after value
            firstStep.setAfter(originalAfter);
        }
    }

    @Override
    public String toString() {
        WorkflowEvent event = workflowContext.getEvent();
        return "eventType=" + event.getOperation() +
                ",resourceType=" + event.getResourceType() +
                ",resourceId=" + event.getResourceId();
    }
}
