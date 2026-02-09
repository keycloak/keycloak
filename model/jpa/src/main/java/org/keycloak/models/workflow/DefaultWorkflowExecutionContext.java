package org.keycloak.models.workflow;

import java.util.UUID;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;

import org.jboss.logging.Logger;

final class DefaultWorkflowExecutionContext implements WorkflowExecutionContext {

    private static final Logger log = Logger.getLogger(DefaultWorkflowExecutionContext.class);

    private final String resourceId;
    private final String executionId;
    private final Workflow workflow;
    private final WorkflowEvent event;
    private final KeycloakSession session;
    private final WorkflowStep step;
    private boolean completed;

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
     * A new execution context for a workflow event, resuming a previously scheduled step. The execution ID is taken from the scheduled step
     * with no current step, indicating that the workflow is being restarted due to an event.
     *
     * @param workflow the workflow
     * @param event the event
     * @param step the scheduled step
     */
    DefaultWorkflowExecutionContext(KeycloakSession session, Workflow workflow, WorkflowEvent event, ScheduledStep step) {
        this(session, workflow, event, null, step.executionId(), event.getResourceId());
    }

    /**
     * A execution context for a scheduled step, resuming the workflow from that step. The execution ID is taken from the scheduled step.
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
        this(session, context.getWorkflow(), context.getEvent(), context.getCurrentStepId(), context.getExecutionId(), context.getResourceId());
        completed = context.isCompleted();
    }

    /**
     * A copy constructor that creates a new execution context based on an existing one but bound to a different {@link KeycloakSession} and the given {@link WorkflowStep}.

     * @param session the session
     * @param context the existing context
     * @param step the current step
     */
    DefaultWorkflowExecutionContext(KeycloakSession session, DefaultWorkflowExecutionContext context, WorkflowStep step) {
        this(session, context.getWorkflow(), context.getEvent(), step.getId(), context.getExecutionId(), context.getResourceId());
        completed = context.isCompleted();
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

    boolean isCompleted() {
        return this.completed;
    }

    void complete() {
        completed = true;
    }

    void restart(int position) {
        new RestartWorkflowTask(this, position).run(session);
    }

    KeycloakSession getSession() {
        return session;
    }

    private String getCurrentStepId() {
        return step != null ? step.getId() : null;
    }
}
