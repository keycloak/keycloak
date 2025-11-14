package org.keycloak.models.workflow.conditions.expression;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowConditionProvider;

import static org.keycloak.models.workflow.Workflows.getConditionProvider;

public class PredicateEvaluator extends BooleanConditionParserBaseVisitor<Predicate> {

    private final CriteriaBuilder cb;
    private final CriteriaQuery<String> query;
    private final Root<?> root;
    private final KeycloakSession session;

    public PredicateEvaluator(KeycloakSession session, CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> root) {
        this.session = session;
        this.cb = cb;
        this.query = query;
        this.root = root;
    }

    @Override
    public Predicate visitEvaluator(BooleanConditionParser.EvaluatorContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Predicate visitExpression(BooleanConditionParser.ExpressionContext ctx) {
        // Handle 'expression OR andExpression'
        if (ctx.OR() != null) {
            Predicate left = visit(ctx.expression());
            Predicate right = visit(ctx.andExpression());
            return cb.or(left, right);
        }
        // Handle 'andExpression'
        return visit(ctx.andExpression());
    }

    @Override
    public Predicate visitAndExpression(BooleanConditionParser.AndExpressionContext ctx) {
        // Handle 'andExpression AND notExpression'
        if (ctx.AND() != null) {
            Predicate left = visit(ctx.andExpression());
            Predicate right = visit(ctx.notExpression());
            return cb.and(left, right);
        }
        // Handle 'notExpression'
        return visit(ctx.notExpression());
    }

    @Override
    public Predicate visitNotExpression(BooleanConditionParser.NotExpressionContext ctx) {
        // Handle '!' notExpression
        if (ctx.NOT() != null) {
            return cb.not(visit(ctx.notExpression()));
        }
        // Handle 'atom'
        return visit(ctx.atom());
    }

    @Override
    public Predicate visitAtom(BooleanConditionParser.AtomContext ctx) {
        if (ctx.conditionCall() != null) {
            return visit(ctx.conditionCall());
        }
        return visit(ctx.expression());
    }

    @Override
    public Predicate visitConditionCall(BooleanConditionParser.ConditionCallContext ctx) {
        String conditionName = ctx.Identifier().getText();
        WorkflowConditionProvider conditionProvider = getConditionProvider(session, conditionName, extractParameter(ctx.parameter()));
        return conditionProvider.toPredicate(cb, query, root);
    }

    protected String extractParameter(BooleanConditionParser.ParameterContext paramCtx) {
        // Case 1: No parentheses were used (e.g., "user-logged-in")
        // Case 2: Empty parentheses were used (e.g., "user-logged-in()")
        if (paramCtx == null || paramCtx.ParameterText() == null) {
            return null;
        }

        // Case 3: A parameter was provided (e.g., "has-role(param)")
        String rawText = paramCtx.ParameterText().getText();
        return unEscapeParameter(rawText);
    }

    /**
     * The grammar defines escapes as '\)' and '\\'.
     *
     * @param rawText The raw text from the ParameterText token.
     * @return A clean, un-escaped string.
     */
    private String unEscapeParameter(String rawText) {
        // This handles both \) -> ) and \\ -> \
        // Note: replaceAll uses regex, so we must double-escape the backslashes
        return rawText.replace("\\)", ")")
                .replace("\\\\", "\\");
    }
}
