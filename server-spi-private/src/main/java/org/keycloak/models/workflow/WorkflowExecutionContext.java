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
}
