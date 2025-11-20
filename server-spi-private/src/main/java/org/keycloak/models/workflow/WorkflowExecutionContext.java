package org.keycloak.models.workflow;

/**
 * A contextual object providing information about the workflow execution.
 */
public interface WorkflowExecutionContext {

    /**
     * Returns the id of the resource bound to the current workflow execution.
     *
     * @return the id of the resource
     */
    String getResourceId();

    /**
     * Returns the workflow event that activated the current workflow execution. Can be null if the execution is being
     * resumed from a scheduled step.
     *
     * @return the event bound to the current execution.
     */
    WorkflowEvent getEvent();
}
