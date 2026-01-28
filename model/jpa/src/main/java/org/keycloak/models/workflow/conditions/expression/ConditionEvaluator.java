package org.keycloak.models.workflow.conditions.expression;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowExecutionContext;

import static org.keycloak.models.workflow.Workflows.getConditionProvider;

public class ConditionEvaluator extends AbstractBooleanEvaluator {

    protected final KeycloakSession session;
    protected final WorkflowExecutionContext context;

    public ConditionEvaluator(KeycloakSession session, WorkflowExecutionContext context) {
        this.session = session;
        this.context = context;
    }

    @Override
    public Boolean visitConditionCall(BooleanConditionParser.ConditionCallContext ctx) {
        String conditionName = ctx.Identifier().getText();
        WorkflowConditionProvider conditionProvider = getConditionProvider(session, conditionName, super.extractParameter(ctx.parameter()));
        return conditionProvider.evaluate(context);
    }

}
