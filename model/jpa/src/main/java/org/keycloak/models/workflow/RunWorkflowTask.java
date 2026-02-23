package org.keycloak.models.workflow;


import java.util.concurrent.TimeoutException;

import org.keycloak.common.util.DurationConverter;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;

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
        WorkflowStep nextStep = runCurrentStep(context);

        while (nextStep != null) {
            WorkflowStateProvider.ScheduleResult result = scheduleStep(session, context, nextStep);
            if (result == WorkflowStateProvider.ScheduleResult.CREATED && context.getEvent() != null) {
                fireWorkflowActivated(session, context);
            }
            boolean isNextStepScheduled = DurationConverter.isPositiveDuration(nextStep.getAfter());
            if (isNextStepScheduled) {
                fireWorkflowStepScheduled(session, context, nextStep);
                return;
            }
            nextStep = runWorkflowStep(context, nextStep);
        }

        // no more steps to run, complete the workflow execution
        completeWorkflowExecution(session, context);
    }

    protected WorkflowStep runCurrentStep(DefaultWorkflowExecutionContext context) {
        if (context.getStep() != null) {
            return runWorkflowStep(context, context.getStep());
        }
        return context.getWorkflow().getSteps().findFirst().orElse(null);
    }

    protected WorkflowStep runWorkflowStep(DefaultWorkflowExecutionContext context, WorkflowStep step) {
        String executionId = context.getExecutionId();
        String resourceId = context.getResourceId();
        Workflow workflow = context.getWorkflow();
        KeycloakSession s = context.getSession();

        log.debugf("Running step %s on resource %s (execution id: %s)", step.getProviderId(), resourceId, executionId);
        try {
            String nextStepId = KeycloakModelUtils.runJobInTransactionWithResult(s.getKeycloakSessionFactory(), s.getContext(), session -> {
                // we need a copy of the context with the new session to run the step provider
                DefaultWorkflowExecutionContext stepContext = new DefaultWorkflowExecutionContext(session, context, step);
                // check if the workflow execution was cancelled before running the step
                checkExecutionCancelled(step);
                getStepProvider(session, step).run(stepContext);
                // now check again if the workflow execution was cancelled after running the step, as the step provider might have taken a long time to execute
                checkExecutionCancelled(step);
                WorkflowStep nextStep = stepContext.getNextStep();
                return nextStep != null ? nextStep.getId() : null;
            }, "Workflow step execution task");
            log.debugf("Step %s completed successfully (execution id: %s)", step.getProviderId(), executionId);
            // Fire workflow step executed event
            WorkflowProviderEvents.fireWorkflowStepExecutedEvent(s, workflow, step, resourceId, executionId);
            return nextStepId != null ? context.getWorkflow().getStepById(nextStepId) : null;
        } catch (Exception e) {
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
            WorkflowProviderEvents.fireWorkflowStepFailedEvent(s, workflow, step, resourceId, executionId, errorMessage);

            throw e;
        }
    }

    private WorkflowStateProvider.ScheduleResult scheduleStep(KeycloakSession session, DefaultWorkflowExecutionContext context, WorkflowStep nextStep) {

        Workflow workflow = context.getWorkflow();
        String resourceId = context.getResourceId();
        String executionId = context.getExecutionId();
        boolean isImmediateStep = !DurationConverter.isPositiveDuration(nextStep.getAfter());

        // we always persist the step state in the database, even if the step doesn't have a time defined, to make sure that the workflow execution
        // can be resumed from this step in case of server failure
        return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            WorkflowStateProvider stateProvider = s.getProvider(WorkflowStateProvider.class);
            // if the step is an immediate step, we set after to a short period to prevent it from being picked up by the workflow execution task
            // while we run it
            try {
                if (isImmediateStep)
                    nextStep.setAfter("1m");
                return stateProvider.scheduleStep(workflow, nextStep, resourceId, executionId);
            } finally {
                if (isImmediateStep)
                    nextStep.setAfter(null);
            }
        }, "Workflow step scheduling task");
    }

    private void fireWorkflowActivated(KeycloakSession session, DefaultWorkflowExecutionContext context) {
        // Fire workflow activated event
        log.debugf("Workflow '%s' activated for resource %s (execution id: %s)", context.getWorkflow().getName(),
                context.getResourceId(), context.getExecutionId());
        WorkflowProviderEvents.fireWorkflowActivatedEvent(session, context.getWorkflow(), context.getEvent().getResourceId(),
                context.getExecutionId(), context.getEvent().getEventProviderId());
    }

    private void fireWorkflowStepScheduled(KeycloakSession session, DefaultWorkflowExecutionContext context, WorkflowStep nextStep) {
        log.debugf("Scheduled step %s to run in %s for resource %s (execution id: %s)",
                nextStep.getProviderId(), nextStep.getAfter(), context.getResourceId(), context.getExecutionId());
        // If a step has a time defined, schedule it and stop processing the other steps of workflow
        long scheduledTime = System.currentTimeMillis() + DurationConverter.parseDuration(nextStep.getAfter()).toMillis();
        // Fire workflow step scheduled event
        WorkflowProviderEvents.fireWorkflowStepScheduledEvent(session, context.getWorkflow(), nextStep, context.getResourceId(), context.getExecutionId(),
                scheduledTime, nextStep.getAfter());
    }

    private void completeWorkflowExecution(KeycloakSession session, DefaultWorkflowExecutionContext context) {
        // workflow execution completed - log message and fire event after removing the entre from the workflow state table
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s -> {;
            WorkflowStateProvider stateProvider = s.getProvider(WorkflowStateProvider.class);
            stateProvider.remove(context.getExecutionId());
        });
        log.debugf("Workflow '%s' completed for resource %s (execution id: %s)", context.getWorkflow().getName(),
                context.getResourceId(), context.getExecutionId());
        // Fire workflow completed event
        WorkflowProviderEvents.fireWorkflowCompletedEvent(session, context.getWorkflow(), context.getResourceId(), context.getExecutionId());
    }

    private void checkExecutionCancelled(WorkflowStep step) {
        Throwable throwable = super.futureCancelled.get();
        if (super.futureCancelled.get() != null) {
            if (throwable instanceof TimeoutException || throwable.getCause() instanceof TimeoutException) {
                throw new RuntimeException("Workflow executor timed out during execution of step " + step.getProviderId(), throwable);
            } else {
                throw new RuntimeException("Workflow executor was cancelled during execution of step " + step.getProviderId(), throwable);
            }
        }
    }
}
