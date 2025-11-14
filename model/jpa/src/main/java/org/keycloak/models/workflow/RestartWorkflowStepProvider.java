package org.keycloak.models.workflow;

public class RestartWorkflowStepProvider implements WorkflowStepProvider {

    @Override
    public void run(WorkflowExecutionContext context) {
        if (context instanceof DefaultWorkflowExecutionContext) {
            ((DefaultWorkflowExecutionContext) context).restart();
        } else {
            throw new IllegalArgumentException("Context must be DefaultWorkflowExecutionContext");
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
