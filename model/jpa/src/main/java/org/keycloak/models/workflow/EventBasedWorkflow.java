package org.keycloak.models.workflow;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CANCEL_IF_RUNNING;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CONDITIONS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ON_EVENT;

import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.conditions.ExpressionWorkflowConditionProvider;
import org.keycloak.models.workflow.conditions.expression.BooleanConditionParser;
import org.keycloak.models.workflow.conditions.expression.EvaluatorUtils;
import org.keycloak.models.workflow.conditions.expression.EventEvaluator;
import org.keycloak.utils.StringUtil;

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

    boolean activateOnEvent(WorkflowEvent event) {
        if (!supports(event.getResourceType())) {
            return false;
        }
        return isActivationEvent(event) && evaluateConditions(event);
    }

    boolean deactivateOnEvent(WorkflowEvent event) {
        // TODO: rework this once we support concurrency/restart-if-running and concurrency/cancel-if-running to use expressions just like activation conditions
        if (!supports(event.getResourceType())) {
            return false;
        }

        List<String> events = model.getConfig().getOrDefault(CONFIG_ON_EVENT, List.of());

        for (String activationEvent : events) {
            ResourceOperationType a = ResourceOperationType.valueOf(activationEvent.toUpperCase());

            if (a.isDeactivationEvent(event.getEvent().getClass())) {
                return !evaluateConditions(event);
            }
        }

        return false;
    }

    boolean resetOnEvent(WorkflowEvent event) {
        return isCancelIfRunning() && evaluateConditions(event);
    }

    private boolean evaluateConditions(WorkflowEvent event) {
        String conditions = getModel().getConfig().getFirst(CONFIG_CONDITIONS);
        if (StringUtil.isBlank(conditions)) {
            return true;
        }
        return new ExpressionWorkflowConditionProvider(getSession(), conditions).evaluate(event);
    }

    private boolean isActivationEvent(WorkflowEvent event) {
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
