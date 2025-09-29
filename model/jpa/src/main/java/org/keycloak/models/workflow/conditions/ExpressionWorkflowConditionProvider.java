package org.keycloak.models.workflow.conditions;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowEvent;
import org.keycloak.models.workflow.WorkflowInvalidStateException;
import org.keycloak.models.workflow.conditions.expression.BooleanConditionEvaluator;
import org.keycloak.models.workflow.conditions.expression.BooleanConditionLexer;
import org.keycloak.models.workflow.conditions.expression.BooleanConditionParser;
import org.keycloak.models.workflow.conditions.expression.BooleanConditionParser.EvaluatorContext;
import org.keycloak.models.workflow.conditions.expression.ErrorListener;
import org.keycloak.models.workflow.conditions.expression.PredicateConditionEvaluator;

import java.util.stream.Collectors;

public class ExpressionWorkflowConditionProvider implements WorkflowConditionProvider {

    private final String expression;
    private final KeycloakSession session;
    private EvaluatorContext evaluatorContext;

    public ExpressionWorkflowConditionProvider(KeycloakSession session, String expression) {
        this.session = session;
        this.expression = expression;
    }

    @Override
    public boolean evaluate(WorkflowEvent event) {
        validate();
        BooleanConditionEvaluator evaluator = new BooleanConditionEvaluator(session, event);
        return evaluator.visit(this.evaluatorContext);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> userRoot) {
        validate();
        PredicateConditionEvaluator evaluator = new PredicateConditionEvaluator(session, cb, query, userRoot);
        return evaluator.visit(this.evaluatorContext);
    }

    @Override
    public void validate() {
        // to properly validate the expression, we need to parse it. We then cache the parsed context if the expression is valid
        // so we don't to parse it again if validate is called again on the same instance of the provider
        if (this.evaluatorContext == null) {
            CharStream charStream = CharStreams.fromString(expression);
            BooleanConditionLexer lexer = new BooleanConditionLexer(charStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            BooleanConditionParser parser = new BooleanConditionParser(tokens);

            // this replaces the standard error listener, storing all parsing errors if the expressions is malformed
            ErrorListener errorListener = new ErrorListener();
            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            // parse the expression and check for errors
            EvaluatorContext context = parser.evaluator();
            if (errorListener.hasErrors()) {
                String lineSeparator = System.lineSeparator();
                String errorDetails = errorListener.getErrorMessages().stream()
                        .map(error -> "- " + error)
                        .collect(Collectors.joining(lineSeparator));

                throw new WorkflowInvalidStateException(String.format("Invalid expression: %s%sError details:%s%s",
                        expression, lineSeparator, lineSeparator, errorDetails));
            }
            this.evaluatorContext = context;
        }
    }

    @Override
    public void close() {
        // no-op, nothing to close
    }
}
