package org.keycloak.models.workflow.conditions.expression;

import java.util.stream.Collectors;

import org.keycloak.models.workflow.WorkflowInvalidStateException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class EvaluatorUtils {

    public static BooleanConditionParser.EvaluatorContext createEvaluatorContext(String expression) {
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
        BooleanConditionParser.EvaluatorContext context = parser.evaluator();
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
}
