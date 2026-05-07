package org.keycloak.models.workflow;


import java.util.concurrent.TimeoutException;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ExecutionResult;
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

    private static final int EXECUTION_LOCK_TIMEOUT_SECS = 300;

    @Override
    public void run(KeycloakSession session) {

        DefaultWorkflowExecutionContext context = new DefaultWorkflowExecutionContext(session, this.context);
        String executionId = context.getExecutionId();

        ClusterProvider clusterProvider = session.getProvider(ClusterProvider.class);
        ExecutionResult<Void> result = clusterProvider.executeIfNotExecuted("wf-exec::" + executionId, EXECUTION_LOCK_TIMEOUT_SECS, () -> {
            if (context.getStep() != null) {
                WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
                WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(
                        context.getWorkflow().getId(), context.getResourceId());
                if (scheduledStep == null || !scheduledStep.stepId().equals(context.getStep().getId())) {
                    log.debugf("Execution %s for resource %s: DB state has changed (expected step %s), skipping",
                            executionId, context.getResourceId(), context.getStep().getProviderId());
                    return null;
                }
            }

            WorkflowStep nextStep = runCurrentStep(context);

            while (nextStep != null) {
                WorkflowStateProvider.ScheduleResult scheduleResult = scheduleStep(session, context, nextStep);
                if (scheduleResult == WorkflowStateProvider.ScheduleResult.CREATED && context.getEvent() != null) {
                    fireWorkflowActivated(session, context);
                }
                boolean isNextStepScheduled = DurationConverter.isPositiveDuration(nextStep.getAfter());
                if (isNextStepScheduled) {
                    fireWorkflowStepScheduled(session, context, nextStep);
                    return null;
                }
                nextStep = runWorkflowStep(context, nextStep);
            }

            completeWorkflowExecution(session, context);
            return null;
        });

        if (!result.isExecuted()) {
            log.debugf("Execution %s for resource %s already in progress on another node, skipping",
                    executionId, context.getResourceId());
        }
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

        return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            WorkflowStateProvider stateProvider = s.getProvider(WorkflowStateProvider.class);
            return stateProvider.scheduleStep(workflow, nextStep, resourceId, executionId);
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
