package org.keycloak.models.workflow;

import java.util.UUID;

import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;

final class DefaultWorkflowExecutionContext implements WorkflowExecutionContext {

    private final String resourceId;
    private final String executionId;
    private final Workflow workflow;
    private WorkflowStep currentStep;
    private boolean restarted;

    /**
     * A new execution context for a workflow event. The execution ID is randomly generated.
     *
     * @param workflow the workflow
     * @param event the event
     */
    public DefaultWorkflowExecutionContext(Workflow workflow, WorkflowEvent event) {
        this(workflow, null, UUID.randomUUID().toString(), event.getResourceId());
    }

    /**
     * A new execution context for a workflow event, resuming a previously scheduled step. The execution ID is taken from the scheduled step
     * with no current step, indicating that the workflow is being restarted due to an event.
     *
     * @param workflow the workflow
     * @param event the event
     * @param step the scheduled step
     */
    public DefaultWorkflowExecutionContext(Workflow workflow, WorkflowEvent event, ScheduledStep step) {
        this(workflow, null, step.executionId(), event.getResourceId());
    }

    /**
     * A execution context for a scheduled step, resuming the workflow from that step. The execution ID is taken from the scheduled step.
     *
     * @param manager the workflows manager
     * @param workflow the workflow
     * @param step the scheduled step
     */
    public DefaultWorkflowExecutionContext(WorkflowsManager manager, Workflow workflow, ScheduledStep step) {
        this(workflow, manager.getStepById(workflow, step.stepId()), step.executionId(), step.resourceId());
    }

    private DefaultWorkflowExecutionContext(Workflow workflow, WorkflowStep currentStep, String executionId, String resourceId) {
        this.workflow = workflow;
        this.currentStep = currentStep;
        this.executionId = executionId;
        this.resourceId = resourceId;
    }

    @Override
    public String getResourceId() {
        return resourceId;
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

    boolean restarted() {
        return this.restarted;
    }

    void restart() {
        this.restarted = true;
    }

    void resetState() {
        this.restarted = false;
        this.currentStep = null;
    }
}

