package org.keycloak.models.workflow.conditions.expression;

import java.util.ArrayList;
import java.util.List;

/**
 * This visitor traverses the entire parse tree and collects the names of all conditionCalls.
 */
public class ConditionNameCollector extends BooleanConditionParserBaseVisitor<Void> {

    // 1. A list to store the names we find.
    private final List<String> conditionNames = new ArrayList<>();

    /**
     * Returns the list of all collected condition call names.
     */
    public List<String> getConditionNames() {
        return conditionNames;
    }

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

    // --- The Collector Method ---

    @Override
    public Void visitConditionCall(BooleanConditionParser.ConditionCallContext ctx) {
        String conditionName = ctx.Identifier().getText();
        conditionNames.add(conditionName);

        // We don't need to visit children (like 'parameter')
        return null;
    }
}
