package org.keycloak.models.workflow.expression;

import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.workflow.WorkflowInvalidStateException;
import org.keycloak.models.workflow.expression.BooleanConditionParser.EvaluatorContext;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class EvaluatorUtils {

    /**
     * Creates an EvaluatorContext from the given expression. If the expression is invalid, a WorkflowInvalidStateException
     * is thrown with details about the parsing errors.
     *
     * @param expression the boolean expression to parse
     * @return the EvaluatorContext representing the parsed expression
     */
    public static EvaluatorContext createEvaluatorContext(String expression) {
        // to properly validate the expression, we need to parse it.
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
        return context;
    }

    /**
     * Creates or retrieves a cached EvaluatorContext for the given workflow model and expression.
     *
     * @param workflowModel the workflow component model
     * @param expression   the boolean expression to parse
     * @return the EvaluatorContext representing the parsed expression
     */
    public static EvaluatorContext createEvaluatorContext(ComponentModel workflowModel, String expression) {
        EvaluatorContext context = workflowModel.getNote(expression);
        if (context == null) {
            context = createEvaluatorContext(expression);
            workflowModel.setNote(expression, context);
        }
        return context;
    }
}
