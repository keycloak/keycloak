package org.keycloak.models.workflow;

import java.util.UUID;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;

final class DefaultWorkflowExecutionContext implements WorkflowExecutionContext {

    private final String resourceId;
    private final String executionId;
    private final Workflow workflow;
    private final WorkflowEvent event;
    private final KeycloakSession session;
    private final WorkflowStep step;
    private Integer restartPosition;

    /**
     * A new execution context for a workflow event. The execution ID is randomly generated.
     *
     * @param workflow the workflow
     * @param event the event
     */
    DefaultWorkflowExecutionContext(KeycloakSession session, Workflow workflow, WorkflowEvent event) {
        this(session, workflow, event, null, UUID.randomUUID().toString(), event.getResourceId());
    }

    /**
     * A new execution context for a workflow event. The execution ID is provided as a parameter
     *
     * @param workflow the workflow
     * @param event the event
     * @param executionId the execution ID
     */
    DefaultWorkflowExecutionContext(KeycloakSession session, Workflow workflow, WorkflowEvent event, String executionId) {
        this(session, workflow, event, null, executionId, event.getResourceId());
    }

    /**
     * An execution context for a scheduled step, resuming the workflow from that step. The execution ID and resource ID are
     * taken from the scheduled step.
     *
     * @param session the session
     * @param workflow the workflow
     * @param step the scheduled step
     */
    DefaultWorkflowExecutionContext(KeycloakSession session, Workflow workflow, ScheduledStep step) {
        this(session, workflow, null, step.stepId(), step.executionId(), step.resourceId());
    }

    /**
     * A copy constructor that creates a new execution context based on an existing one but bound to a different {@link KeycloakSession}.

     * @param session the session
     * @param context the existing context
     */
    DefaultWorkflowExecutionContext(KeycloakSession session, DefaultWorkflowExecutionContext context) {
        this(session, new Workflow(session, context.getWorkflow()), context.getEvent(), context.getCurrentStepId(), context.getExecutionId(), context.getResourceId());
    }

    /**
     * A copy constructor that creates a new execution context based on an existing one but bound to a different {@link KeycloakSession} and the given {@link WorkflowStep}.

     * @param session the session
     * @param context the existing context
     * @param step the current step
     */
    DefaultWorkflowExecutionContext(KeycloakSession session, DefaultWorkflowExecutionContext context, WorkflowStep step) {
        this(session, new Workflow(session, context.getWorkflow()), context.getEvent(), step.getId(), context.getExecutionId(), context.getResourceId());
    }

    private DefaultWorkflowExecutionContext(KeycloakSession session, Workflow workflow, WorkflowEvent event, String stepId, String executionId, String resourceId) {
        this.session = session;
        this.workflow = workflow;
        this.event = event;

        if (stepId != null) {
            this.step = workflow.getStepById(stepId);
        } else {
            this.step = null;
        }

        this.executionId = executionId;
        this.resourceId = resourceId;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public WorkflowEvent getEvent() {
        return event;
    }

    @Override
    public WorkflowStep getNextStep() {
        if (restartPosition != null) {
            // we are restarting the workflow, so we ignore the current step and return the step at the restart position
            return workflow.getSteps().skip(restartPosition).findFirst().orElse(null);
        }
        return workflow.getSteps(step.getId()).skip(1).findFirst().orElse(null);
    }

    String getExecutionId() {
        return this.executionId;
    }

    Workflow getWorkflow() {
        return workflow;
    }

    WorkflowStep getStep() {
        return step;
    }

    void restart(int position) {
        this.restartPosition = position;
    }

    KeycloakSession getSession() {
        return session;
    }

    private String getCurrentStepId() {
        return step != null ? step.getId() : null;
    }
}
