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
    private WorkflowStep currentStep;
    private boolean restarted;

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
        this(session, workflow, null, step, step.executionId(), step.resourceId());
    }

    private DefaultWorkflowExecutionContext(KeycloakSession session, Workflow workflow, WorkflowEvent event, ScheduledStep step, String executionId, String resourceId) {
        this.session = session;
        this.workflow = workflow;
        this.event = event;

        if (step != null) {
            this.currentStep = workflow.getStepById(step.stepId());
        } else {
            this.currentStep = null;
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

    String getExecutionId() {
        return this.executionId;
    }

    Workflow getWorkflow() {
        return workflow;
    }

    WorkflowStep getCurrentStep() {
        return currentStep;
    }

    void setCurrentStep(WorkflowStep step) {
        this.currentStep = step;
    }

    boolean isRestarted() {
        return this.restarted;
    }

    void restart() {
        log.debugf("Restarting workflow '%s' for resource %s (execution id: %s)", workflow.getName(), resourceId, executionId);
        this.restarted = false;
        this.currentStep = null;
        new RunWorkflowTask(this).run(session);
        this.restarted = true;
    }

    KeycloakSession getSession() {
        return session;
    }
}
