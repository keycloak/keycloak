package org.keycloak.models.workflow;


import org.keycloak.common.util.DurationConverter;
import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

import static org.keycloak.models.workflow.Workflows.getStepProvider;

class RunWorkflowTask extends WorkflowTransactionalTask {

    private static final Logger log = Logger.getLogger(RunWorkflowTask.class);

    protected final DefaultWorkflowExecutionContext context;

    RunWorkflowTask(DefaultWorkflowExecutionContext context) {
        super(context.getSession());
        this.context = context;
    }

    @Override
    public void run(KeycloakSession session) {
        DefaultWorkflowExecutionContext context = new DefaultWorkflowExecutionContext(session, this.context);
        WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
        Workflow workflow = context.getWorkflow();
        String resourceId = context.getResourceId();
        String executionId = context.getExecutionId();
        WorkflowStep nextStep = runCurrentStep(context);

        while (nextStep != null) {
            if (DurationConverter.isPositiveDuration(nextStep.getAfter())) {
                log.debugf("Scheduling step %s to run in %s for resource %s (execution id: %s)",
                        nextStep.getProviderId(), nextStep.getAfter(), resourceId, executionId);
                // If a step has a time defined, schedule it and stop processing the other steps of workflow
                long scheduledTime = System.currentTimeMillis() + DurationConverter.parseDuration(nextStep.getAfter()).toMillis();
                stateProvider.scheduleStep(workflow, nextStep, resourceId, executionId);

                // Fire workflow step scheduled event
                WorkflowProviderEvents.fireWorkflowStepScheduledEvent(session, workflow, nextStep, resourceId, executionId,
                        scheduledTime, nextStep.getAfter());
                return;
            }

            DefaultWorkflowExecutionContext stepContext = new DefaultWorkflowExecutionContext(session, this.context, nextStep);

            nextStep = runWorkflowStep(stepContext);

            if (stepContext.isCompleted()) {
                return;
            }
        }

        // not recurring, remove the state record
        log.debugf("Workflow '%s' completed for resource %s (execution id: %s)", workflow.getName(), resourceId, executionId);

        // Fire workflow completed event
        WorkflowProviderEvents.fireWorkflowCompletedEvent(session, workflow, resourceId, executionId);

        stateProvider.remove(executionId);
    }

    protected WorkflowStep runCurrentStep(DefaultWorkflowExecutionContext context) {
        if (context.getStep() != null) {
            return runWorkflowStep(context);
        }
        return context.getWorkflow().getSteps().findFirst().orElse(null);
    }

    private WorkflowStep runWorkflowStep(DefaultWorkflowExecutionContext context) {
        WorkflowStep step = context.getStep();
        String executionId = context.getExecutionId();
        String resourceId = context.getResourceId();
        Workflow workflow = context.getWorkflow();
        KeycloakSession session = context.getSession();

        log.debugf("Running step %s on resource %s (execution id: %s)", step.getProviderId(), resourceId, executionId);
        try {
            getStepProvider(session, step).run(context);
            log.debugf("Step %s completed successfully (execution id: %s)", step.getProviderId(), executionId);

            // Fire workflow step executed event
            WorkflowProviderEvents.fireWorkflowStepExecutedEvent(session, workflow, step, resourceId, executionId);
        } catch (WorkflowExecutionException e) {
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

            // Fire workflow step failed event
            WorkflowProviderEvents.fireWorkflowStepFailedEvent(session, workflow, step, resourceId, executionId, errorMessage);

            throw e;
        }

        return context.getNextStep();
    }
}
