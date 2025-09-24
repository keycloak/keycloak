package org.keycloak.models.workflow.conditions.expression;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.antlr.v4.runtime.tree.ParseTree;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;
import org.keycloak.models.workflow.WorkflowsManager;

import java.util.List;
import java.util.stream.Collectors;

public class PredicateConditionEvaluator extends BooleanConditionBaseVisitor<Predicate> {

    private final KeycloakSession session;
    private final CriteriaBuilder cb;
    private final CriteriaQuery<String> query;
    private final Root<?> root;
    private WorkflowsManager manager;

    public PredicateConditionEvaluator(KeycloakSession session, CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> root) {
        this.session = session;
        this.cb = cb;
        this.query = query;
        this.root = root;
        this.manager = new WorkflowsManager(session);
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
        // Handle '!' notExpression'
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
        WorkflowConditionProviderFactory<WorkflowConditionProvider> providerFactory = manager.getConditionProviderFactory(conditionName);
        WorkflowConditionProvider conditionProvider = providerFactory.create(session, extractParameterList(ctx.parameterList()));
        return conditionProvider.toPredicate(cb, query, root);
    }

    private List<String> extractParameterList(BooleanConditionParser.ParameterListContext ctx) {
        if (ctx == null) {
            return List.of();
        }
        return ctx.StringLiteral().stream()
                .map(this::visitStringLiteral)
                .collect(Collectors.toList());
    }

    private String visitStringLiteral(ParseTree ctx) {
        String text = ctx.getText();
        return text.substring(1, text.length() - 1).replace("\"\"", "\"");
    }
}
