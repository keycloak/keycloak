package org.keycloak.models.workflow.conditions;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.models.workflow.conditions.expression.BooleanConditionParser.EvaluatorContext;
import org.keycloak.models.workflow.conditions.expression.ConditionEvaluator;
import org.keycloak.models.workflow.conditions.expression.EvaluatorUtils;
import org.keycloak.models.workflow.conditions.expression.PredicateEvaluator;

public class ExpressionWorkflowConditionProvider implements WorkflowConditionProvider {

    private final String expression;
    private final KeycloakSession session;
    private EvaluatorContext evaluatorContext;

    public ExpressionWorkflowConditionProvider(KeycloakSession session, String expression) {
        this.session = session;
        this.expression = expression;
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        validate();
        ConditionEvaluator evaluator = new ConditionEvaluator(session, context);
        return evaluator.visit(this.evaluatorContext);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> userRoot) {
        validate();
        PredicateEvaluator evaluator = new PredicateEvaluator(session, cb, query, userRoot);
        return evaluator.visit(this.evaluatorContext);
    }

    @Override
    public void validate() {
        if (this.evaluatorContext == null) {
            this.evaluatorContext = EvaluatorUtils.createEvaluatorContext(expression);
        }
    }

    @Override
    public void close() {
        // no-op, nothing to close
    }
}
