package org.keycloak.models.workflow.conditions.expression;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.WorkflowEvent;

public class EventEvaluator extends AbstractBooleanEvaluator {

    private final WorkflowEvent event;

    public EventEvaluator(KeycloakSession session, WorkflowEvent event) {
        this.event = event;
    }

    @Override
    public Boolean visitConditionCall(BooleanConditionParser.ConditionCallContext ctx) {
        String name = ctx.Identifier().getText();
        ResourceOperationType operation = ResourceOperationType.valueOf(name.replace("-", "_").toUpperCase());
        String param = super.extractParameter(ctx.parameter());
        return operation.test(event, param);
    }
}
