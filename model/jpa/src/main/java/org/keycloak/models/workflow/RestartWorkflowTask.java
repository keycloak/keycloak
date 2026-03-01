package org.keycloak.models.workflow;

import java.util.List;

import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

class RestartWorkflowTask extends RunWorkflowTask {

    private static final Logger log = Logger.getLogger(RestartWorkflowTask.class);

    private final int position;

    RestartWorkflowTask(DefaultWorkflowExecutionContext context, int position) {
        super(context);
        this.position = position;
    }

    @Override
    protected WorkflowStep runCurrentStep(DefaultWorkflowExecutionContext context) {
        Workflow workflow = context.getWorkflow();
        List<WorkflowStep> steps = workflow.getSteps().toList();

        if (position < 0 || position >= steps.size()) {
            throw new IllegalArgumentException("Invalid position to restart workflow: " + position);
        }

        return steps.get(position);
    }

    @Override
    public void run(KeycloakSession session) {
        if (log.isDebugEnabled()) {
            Workflow workflow = context.getWorkflow();
            String resourceId = context.getResourceId();
            String executionId = context.getExecutionId();
            WorkflowStep currentStep = context.getStep();

            if (currentStep == null) {
                currentStep = workflow.getSteps().findFirst().orElse(null);
            }

            log.debugf("Restarting workflow '%s' for resource %s (execution id: %s) at step %s", workflow.getName(), resourceId, executionId, currentStep.getProviderId());
        }
        super.run(session);
    }
}
