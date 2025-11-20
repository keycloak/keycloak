package org.keycloak.models.workflow;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.conditions.ExpressionWorkflowConditionProvider;
import org.keycloak.models.workflow.conditions.expression.BooleanConditionParser;
import org.keycloak.models.workflow.conditions.expression.EvaluatorUtils;
import org.keycloak.models.workflow.conditions.expression.EventEvaluator;
import org.keycloak.utils.StringUtil;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CANCEL_IF_RUNNING;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CONDITIONS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ON_EVENT;

final class EventBasedWorkflow {

    private final KeycloakSession session;
    private final ComponentModel model;

    EventBasedWorkflow(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
    }

    boolean supports(ResourceType type) {
        return ResourceType.USERS.equals(type);
    }

    /**
     * Evaluates the specified context to determine whether the workflow should be activated or not. Activation will happen
     * if the context's event matches the configured activation events and the resource conditions evaluate to true.
     *
     * @param executionContext
     * @return
     * @throws WorkflowInvalidStateException
     */
    boolean activate(WorkflowExecutionContext executionContext) throws WorkflowInvalidStateException {
        WorkflowEvent event = executionContext.getEvent();
        if (event == null) {
            return false;
        }
        return supports(event.getResourceType()) && activateOnEvent(event) && validateResourceConditions(executionContext);
    }

    boolean deactivate(WorkflowExecutionContext executionContext) throws WorkflowInvalidStateException {
        // TODO: rework this once we support concurrency/restart-if-running and concurrency/cancel-if-running to use expressions just like activation conditions
        return false;
    }

    boolean reset(WorkflowExecutionContext executionContext) throws WorkflowInvalidStateException {
        WorkflowEvent event = executionContext.getEvent();
        if (event == null) {
            return false;
        }
        return supports(event.getResourceType()) && isCancelIfRunning() && validateResourceConditions(executionContext);
    }

    public boolean validateResourceConditions(WorkflowExecutionContext context) {
        String conditions = getModel().getConfig().getFirst(CONFIG_CONDITIONS);
        if (StringUtil.isBlank(conditions)) {
            return true;
        }
        return new ExpressionWorkflowConditionProvider(getSession(), conditions).evaluate(context);
    }

    /**
     * Determins whether the workflow should be activated based on the given event or not.
     *
     * @param event a reference to the workflow event.
     * @return {@code true} if the workflow should be activated, {@code false} otherwise.
     */
    private boolean activateOnEvent(WorkflowEvent event) {
        // AD_HOC is a special case that always triggers the workflow regardless of the configured activation events
        if (ResourceOperationType.AD_HOC.equals(event.getOperation())) {
            return true;
        }

        String eventConditions = model.getConfig().getFirst(CONFIG_ON_EVENT);
        if (StringUtil.isNotBlank(eventConditions)) {
            BooleanConditionParser.EvaluatorContext context = EvaluatorUtils.createEvaluatorContext(eventConditions);
            EventEvaluator eventEvaluator = new EventEvaluator(getSession(), event);
            return eventEvaluator.visit(context);
        } else {
            return false;
        }
    }

    private ComponentModel getModel() {
        return model;
    }

    private KeycloakSession getSession() {
        return session;
    }

    private boolean isCancelIfRunning() {
       return Boolean.parseBoolean(model.getConfig().getFirstOrDefault(CONFIG_CANCEL_IF_RUNNING, "false"));
    }
}
