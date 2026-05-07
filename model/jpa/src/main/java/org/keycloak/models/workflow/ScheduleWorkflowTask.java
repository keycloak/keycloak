package org.keycloak.models.workflow;

import org.keycloak.common.util.DurationConverter;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.jboss.logging.Logger;

final class ScheduleWorkflowTask extends WorkflowTransactionalTask {

    private static final Logger log = Logger.getLogger(ScheduleWorkflowTask.class);
    private final DefaultWorkflowExecutionContext context;

    ScheduleWorkflowTask(DefaultWorkflowExecutionContext context) {
        super(context.getSession());
        this.context = context;
    }

    @Override
    public void run(KeycloakSession session) {
        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        if (realm == null) {
            // during startup realms might be created/imported - skip processing in this case
            return;
        }

        DefaultWorkflowExecutionContext workflowContext = new DefaultWorkflowExecutionContext(session, this.context);
        Workflow workflow = workflowContext.getWorkflow();
        WorkflowEvent event = workflowContext.getEvent();
        WorkflowStep firstStep = workflow.getSteps().findFirst().orElseThrow(() -> new WorkflowInvalidStateException("No steps found for workflow " + workflow.getName()));
        log.debugf("Scheduling first step '%s' of workflow '%s' for resource %s based on on event %s with notBefore %d",
                firstStep.getProviderId(), workflow.getName(), event.getResourceId(), event.getEventProviderId(), workflow.getNotBefore());
        String originalAfter = firstStep.getAfter();
        try {
            firstStep.setAfter(workflow.getNotBefore());
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            stateProvider.scheduleStep(workflow, firstStep, event.getResourceId(), workflowContext.getExecutionId());
            fireWorkflowActivated(session, workflowContext);
            fireWorkflowStepScheduled(session, workflowContext, firstStep);
        } finally {
            // restore the original after value
            firstStep.setAfter(originalAfter);
        }
    }

    private void fireWorkflowActivated(KeycloakSession session, DefaultWorkflowExecutionContext context) {
        log.debugf("Workflow '%s' activated for resource %s (execution id: %s)", context.getWorkflow().getName(),
                context.getResourceId(), context.getExecutionId());
        // fire workflow activated event
        WorkflowProviderEvents.fireWorkflowActivatedEvent(session, context.getWorkflow(), context.getEvent().getResourceId(),
                context.getExecutionId(), context.getEvent().getEventProviderId());
    }

    private void fireWorkflowStepScheduled(KeycloakSession session, DefaultWorkflowExecutionContext context, WorkflowStep nextStep) {
        log.debugf("Scheduled step %s to run in %s for resource %s (execution id: %s)",
                nextStep.getProviderId(), nextStep.getAfter(), context.getResourceId(), context.getExecutionId());
        long scheduledTime = System.currentTimeMillis() + DurationConverter.parseDuration(nextStep.getAfter()).toMillis();
        // fire workflow step scheduled event
        WorkflowProviderEvents.fireWorkflowStepScheduledEvent(session, context.getWorkflow(), nextStep, context.getResourceId(), context.getExecutionId(),
                scheduledTime, nextStep.getAfter());
    }

    @Override
    public String toString() {
        WorkflowEvent event = context.getEvent();
        return "eventType=" + event.getEventProviderId() +
                ",resourceType=" + event.getResourceType() +
                ",resourceId=" + event.getResourceId();
    }
}
