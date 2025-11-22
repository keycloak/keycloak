package org.keycloak.models.workflow.conditions.expression;

abstract class AbstractConditionCollector extends BooleanConditionParserBaseVisitor<Void>  {

    // --- Traversal Methods ---
    // These methods are necessary to ensure we visit every node in the tree.

    @Override
    public Void visitEvaluator(BooleanConditionParser.EvaluatorContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Void visitExpression(BooleanConditionParser.ExpressionContext ctx) {
        // Visit both sides of the 'OR'
        if (ctx.expression() != null) {
            visit(ctx.expression());
        }
        return visit(ctx.andExpression());
    }

    @Override
    public Void visitAndExpression(BooleanConditionParser.AndExpressionContext ctx) {
        // Visit both sides of the 'AND'
        if (ctx.andExpression() != null) {
            visit(ctx.andExpression());
        }
        return visit(ctx.notExpression());
    }

    @Override
    public Void visitNotExpression(BooleanConditionParser.NotExpressionContext ctx) {
        // Visit the inner expression of the 'NOT'
        if (ctx.notExpression() != null) {
            return visit(ctx.notExpression());
        }
        return visit(ctx.atom());
    }

    @Override
    public Void visitAtom(BooleanConditionParser.AtomContext ctx) {
        // This is the key: decide whether to visit a conditionCall
        // or a nested expression.
        if (ctx.conditionCall() != null) {
            return visit(ctx.conditionCall());
        }
        return visit(ctx.expression());
    }
}
