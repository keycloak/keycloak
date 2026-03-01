package org.keycloak.models.workflow;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.expression.BooleanConditionParser;
import org.keycloak.models.workflow.expression.ConditionEvaluator;
import org.keycloak.models.workflow.expression.EvaluatorUtils;
import org.keycloak.models.workflow.expression.EventEvaluator;
import org.keycloak.representations.workflows.WorkflowConstants;
import org.keycloak.utils.StringUtil;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CANCEL_IN_PROGRESS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CONDITIONS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ON_EVENT;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_RESTART_IN_PROGRESS;

final class EventBasedWorkflow {

    private final KeycloakSession session;
    private final ResourceType supportedType;
    private final ComponentModel model;

    EventBasedWorkflow(KeycloakSession session, ResourceType supportedType, ComponentModel model) {
        this.supportedType = supportedType;
        this.session = session;
        this.model = model;
    }

    boolean supports(ResourceType type) {
        return supportedType == type;
    }

    /**
     * Evaluates the specified context to determine whether the workflow should be activated or not. Activation will happen
     * if the context's event matches the configured activation events and the resource conditions evaluate to true.
     *
     * @param executionContext a reference to the workflow execution context.
     * @return {@code true} if the workflow should be activated, {@code false} otherwise.
     */
    boolean activate(WorkflowExecutionContext executionContext) {
        WorkflowEvent event = executionContext.getEvent();
        if (event == null) {
            return false;
        }
        return supports(event.getResourceType()) && activateOnEvent(executionContext) && validateResourceConditions(executionContext);
    }

    /**
     * Evaluates the specified context to determine whether the workflow should be deactivated or not. Deactivation will happen
     * if the context's event matches the configured cancel-in-progress setting.
     *
     * @param executionContext a reference to the workflow execution context.
     * @return {@code true} if the workflow should be deactivated, {@code false} otherwise.
     */
    boolean deactivate(WorkflowExecutionContext executionContext) {
        String cancelInProgress = model.getConfig().getFirst(CONFIG_CANCEL_IN_PROGRESS);
        return matchesConcurrencySetting(executionContext, cancelInProgress);
    }

    /**
     * Evaluates the specified context to determine whether the workflow should be restarted or not. Restart will happen
     * if the context's event matches the configured restart-in-progress setting.
     *
     * @param executionContext a reference to the workflow execution context.
     * @return {@code true} if the workflow should be restarted, {@code false} otherwise.
     */
    boolean restart(WorkflowExecutionContext executionContext) {
        String restartInProgress = model.getConfig().getFirst(CONFIG_RESTART_IN_PROGRESS);
        return matchesConcurrencySetting(executionContext, restartInProgress);
    }

    /**
     * Validates the resource conditions defined in the workflow configuration against the given execution context.
     * If no conditions are defined, the method returns {@code true}.
     *
     * @param context a reference to the workflow execution context.
     * @return {@code true} if the resource conditions are met or not defined, {@code false} otherwise.
     */
    public boolean validateResourceConditions(WorkflowExecutionContext context) {
        String conditions = getModel().getConfig().getFirst(CONFIG_CONDITIONS);
        if (StringUtil.isNotBlank(conditions)) {
            BooleanConditionParser.EvaluatorContext evaluatorContext = EvaluatorUtils.createEvaluatorContext(model, conditions);
            ConditionEvaluator evaluator = new ConditionEvaluator(session, context);
            return evaluator.visit(evaluatorContext);
        } else {
            return true;
        }
    }

    /**
     * Determines whether the workflow should be activated based on the given event or not.
     *
     * @param executionContext a reference to the workflow execution context.
     * @return {@code true} if the workflow should be activated, {@code false} otherwise.
     */
    private boolean activateOnEvent(WorkflowExecutionContext executionContext) {
        // AD_HOC is a special case that always triggers the workflow regardless of the configured activation events
        if (WorkflowConstants.AD_HOC.equals(executionContext.getEvent().getEventProviderId())) {
            return true;
        }

        String eventConditions = model.getConfig().getFirst(CONFIG_ON_EVENT);
        if (StringUtil.isNotBlank(eventConditions)) {
            BooleanConditionParser.EvaluatorContext context = EvaluatorUtils.createEvaluatorContext(model, eventConditions);
            EventEvaluator eventEvaluator = new EventEvaluator(session, executionContext);
            return eventEvaluator.visit(context);
        } else {
            return false;
        }
    }

    /**
     * Determines whether the event in the given execution context matches the concurrency setting, which can be one of
     * {@code restart-in-progress} or {@code cancel-in-progress}. If the setting is set to "true", the decision is based
     * on the activation settings. If the setting contains an event expression, it is parsed and evaluated.
     *
     * @param executionContext a reference to the workflow execution context.
     * @param concurrencySetting the concurrency setting to evaluate.
     * @return {@code true} if the event matches the concurrency setting, {@code false} otherwise.
     */
    private boolean matchesConcurrencySetting(WorkflowExecutionContext executionContext, String concurrencySetting) {
        WorkflowEvent event = executionContext.getEvent();
        if (event == null) {
            return false;
        }

        if (StringUtil.isNotBlank(concurrencySetting)) {
            // if the setting is "true", we decide based on the activation conditions but only if the workflow has activation events configured
            if (Boolean.parseBoolean(concurrencySetting)) {
                return StringUtil.isNotBlank(model.getConfig().getFirst(CONFIG_ON_EVENT)) && activate(executionContext);
            }
            else {
                // the flag has an event expression - parse and evaluate it
                BooleanConditionParser.EvaluatorContext context = EvaluatorUtils.createEvaluatorContext(model, concurrencySetting);
                EventEvaluator eventEvaluator = new EventEvaluator(session, executionContext);
                return eventEvaluator.visit(context);
            }
        }
        return false;
    }

    private ComponentModel getModel() {
        return model;
    }

    private KeycloakSession getSession() {
        return session;
    }
}
