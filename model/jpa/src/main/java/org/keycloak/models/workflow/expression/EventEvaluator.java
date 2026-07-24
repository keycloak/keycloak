package org.keycloak.models.workflow.expression;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowEventProvider;
import org.keycloak.models.workflow.WorkflowExecutionContext;

import static org.keycloak.models.workflow.Workflows.getEventProvider;

public class EventEvaluator extends AbstractBooleanEvaluator {

    private final WorkflowExecutionContext context;
    private final KeycloakSession session;

    public EventEvaluator(KeycloakSession session, WorkflowExecutionContext context) {
        this.context = context;
        this.session = session;
    }

    @Override
    public Boolean visitConditionCall(BooleanConditionParser.ConditionCallContext ctx) {
        String name = ctx.Identifier().getText();
        WorkflowEventProvider provider = getEventProvider(session, name.replace("_", "-").toLowerCase(), super.extractParameter(ctx.parameter()));
        return provider.evaluate(context);
    }
}
