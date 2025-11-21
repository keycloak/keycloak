package org.keycloak.models.workflow.conditions.expression;

public abstract class AbstractBooleanEvaluator extends BooleanConditionParserBaseVisitor<Boolean> {

    @Override
    public Boolean visitEvaluator(BooleanConditionParser.EvaluatorContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Boolean visitExpression(BooleanConditionParser.ExpressionContext ctx) {
        if (ctx.expression() != null && ctx.OR() != null) {
            return visit(ctx.expression()) || visit(ctx.andExpression());
        }
        return visit(ctx.andExpression());
    }

    @Override
    public Boolean visitAndExpression(BooleanConditionParser.AndExpressionContext ctx) {
        if (ctx.andExpression() != null && ctx.AND() != null) {
            return visit(ctx.andExpression()) && visit(ctx.notExpression());
        }
        return visit(ctx.notExpression());
    }

    @Override
    public Boolean visitNotExpression(BooleanConditionParser.NotExpressionContext ctx) {
        if (ctx.NOT() != null) {
            return !visit(ctx.notExpression());
        }
        return visit(ctx.atom());
    }

    @Override
    public Boolean visitAtom(BooleanConditionParser.AtomContext ctx) {
        if (ctx.conditionCall() != null) {
            return visit(ctx.conditionCall());
        }
        return visit(ctx.expression());
    }

    @Override
    public abstract Boolean visitConditionCall(BooleanConditionParser.ConditionCallContext ctx);

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
