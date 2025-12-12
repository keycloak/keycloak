package org.keycloak.models.workflow;

public record RestartWorkflowStepProvider(int position) implements WorkflowStepProvider {

    @Override
    public void run(WorkflowExecutionContext context) {
        if (context instanceof DefaultWorkflowExecutionContext) {
            ((DefaultWorkflowExecutionContext) context).restart(position);
        } else {
            throw new IllegalArgumentException("Context must be DefaultWorkflowExecutionContext");
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
