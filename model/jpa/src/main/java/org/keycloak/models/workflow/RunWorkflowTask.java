package org.keycloak.models.workflow;

import java.util.List;

import org.keycloak.common.util.DurationConverter;
import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

class RunWorkflowTask extends WorkflowTransactionalTask {

    private static final Logger log = Logger.getLogger(RunWorkflowTask.class);

    private final DefaultWorkflowExecutionContext context;

    RunWorkflowTask(DefaultWorkflowExecutionContext context) {
        super(context.getSession());
        this.context = context;
    }

    @Override
    public void run(KeycloakSession session) {
        DefaultWorkflowProvider provider = (DefaultWorkflowProvider) session.getProvider(WorkflowProvider.class);
        String executionId = context.getExecutionId();
        String resourceId = context.getResourceId();
        Workflow workflow = context.getWorkflow();
        WorkflowStep currentStep = context.getCurrentStep();

        if (currentStep != null) {
            // we are resuming from a scheduled step - run it and then continue with the rest of the workflow
            runWorkflowStep(provider, context);
        }

        List<WorkflowStep> stepsToRun = workflow.getSteps()
                .skip(currentStep != null ? currentStep.getPriority() : 0).toList();
        WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);

        for (WorkflowStep step : stepsToRun) {
            if (DurationConverter.isPositiveDuration(step.getAfter())) {
                // If a step has a time defined, schedule it and stop processing the other steps of workflow
                log.debugf("Scheduling step %s to run in %d ms for resource %s (execution id: %s)",
                        step.getProviderId(), step.getAfter(), resourceId, executionId);
                stateProvider.scheduleStep(workflow, step, resourceId, executionId);
                return;
            } else {
                // Otherwise, run the step right away
                context.setCurrentStep(step);
                runWorkflowStep(provider, context);
            }
        }
        if (context.isRestarted()) {
            // last step was a restart, so we restart the workflow from the beginning
            context.restart();
            return;
        }

        // not recurring, remove the state record
        log.debugf("Workflow '%s' completed for resource %s (execution id: %s)", workflow.getName(), resourceId, executionId);
        stateProvider.remove(executionId);
    }

    private void runWorkflowStep(DefaultWorkflowProvider provider, DefaultWorkflowExecutionContext context) {
        String executionId = context.getExecutionId();
        WorkflowStep step = context.getCurrentStep();
        String resourceId = context.getResourceId();
        log.debugf("Running step %s on resource %s (execution id: %s)", step.getProviderId(), resourceId, executionId);
        try {
            provider.getStepProvider(step).run(context);
            log.debugf("Step %s completed successfully (execution id: %s)", step.getProviderId(), executionId);
        } catch(WorkflowExecutionException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Step %s failed (execution id: %s)");
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                sb.append(" - error message: %s");
                log.debugf(sb.toString(), step.getProviderId(), executionId, errorMessage);
            }
            else {
                log.debugf(sb.toString(), step.getProviderId(), executionId);
            }
            throw e;
        }
    }
}
