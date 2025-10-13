package org.keycloak.models.workflow;

import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

import static java.util.Optional.ofNullable;

public class WorkflowExecutionContext {

    private static final Logger logger = Logger.getLogger(WorkflowExecutionContext.class);

    private String executionId;
    private String resourceId;
    private Workflow workflow;
    private List<WorkflowStep> steps;

    // variable that keep track of execution steps
    private int lastExecutedStepIndex = -1;

    public WorkflowExecutionContext(Workflow workflow, List<WorkflowStep> steps, String resourceId) {
        this.workflow = workflow;
        this.steps = ofNullable(steps).orElse(List.of());
        this.resourceId = resourceId;
    }

    public WorkflowExecutionContext(Workflow workflow, List<WorkflowStep> steps, String resourceId, String stepId, String executionId) {
        this(workflow, steps, resourceId);
        this.executionId = executionId;
        if (stepId != null) {
            for (int i = 0; i < steps.size(); i++) {
                if (steps.get(i).getId().equals(stepId)) {
                    this.lastExecutedStepIndex = i - 1;
                    break;
                }
            }
        }
    }

    public void init() {
        if (this.executionId == null) {
            this.executionId = UUID.randomUUID().toString();
            logger.debugf("Started workflow '%s' for resource %s (execution id: %s)", this.workflow.getName(), this.resourceId, this.executionId);
        }
    }

    public void success(WorkflowStep step) {
        logger.debugf("Step %s completed successfully (execution id: %s)", step.getProviderId(), executionId);
    }

    public void fail(WorkflowStep step, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Step %s failed (execution id: %s)");
        if (errorMessage != null) {
            sb.append(" - error message: %s");
            logger.debugf(sb.toString(), step.getProviderId(), executionId, errorMessage);
        }
        else {
            logger.debugf(sb.toString(), step.getProviderId(), executionId);
        }
    }

    public void complete() {
        logger.debugf("Workflow '%s' completed for resource %s (execution id: %s)", workflow.getName(), resourceId, executionId);
    }

    public void cancel() {
        logger.debugf("Workflow '%s' cancelled for resource %s (execution id: %s)", workflow.getName(), resourceId, executionId);
    }

    public boolean hasNextStep() {
        return lastExecutedStepIndex + 1 < steps.size();
    }

    public WorkflowStep getNextStep() {
        if (lastExecutedStepIndex + 1 < steps.size()) {
            return steps.get(++lastExecutedStepIndex);
        }
        return null;
    }

    public void restart() {
        logger.debugf("Restarted workflow '%s' for resource %s (execution id: %s)",workflow.getName(), resourceId, executionId);
        this.lastExecutedStepIndex = -1;
    }

    public String getExecutionId() {
        return this.executionId;
    }
}
