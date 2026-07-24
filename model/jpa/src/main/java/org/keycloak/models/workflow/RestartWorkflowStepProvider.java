package org.keycloak.models.workflow;

import java.util.List;

public record RestartWorkflowStepProvider(int position) implements WorkflowStepProvider {

    @Override
    public void run(WorkflowExecutionContext context) {
        if (context instanceof DefaultWorkflowExecutionContext) {
            Workflow workflow = ((DefaultWorkflowExecutionContext) context).getWorkflow();
            List<WorkflowStep> steps = workflow.getSteps().toList();

            if (position < 0 || position >= steps.size()) {
                throw new IllegalStateException("Invalid position to restart workflow: " + position);
            }

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
