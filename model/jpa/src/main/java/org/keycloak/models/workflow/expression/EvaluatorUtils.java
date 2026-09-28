package org.keycloak.models.workflow.expression;

import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.workflow.WorkflowInvalidStateException;
import org.keycloak.models.workflow.expression.BooleanConditionParser.EvaluatorContext;
import org.keycloak.utils.StringUtil;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class EvaluatorUtils {

    public static final int MAX_EXPRESSION_LENGTH = 2048;
    public static final int MAX_EXPRESSION_DEPTH = 10;

    /**
     * Creates an EvaluatorContext from the given expression. If the expression is invalid, a WorkflowInvalidStateException
     * is thrown with details about the parsing errors.
     *
     * @param expression the boolean expression to parse
     * @return the EvaluatorContext representing the parsed expression
     */
    public static EvaluatorContext createEvaluatorContext(String expression) {
        if (StringUtil.isBlank(expression)) {
            throw new WorkflowInvalidStateException("workflowExpressionEmpty");
        }
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            throw new WorkflowInvalidStateException("workflowExpressionMaxLength", MAX_EXPRESSION_LENGTH);
        }
        validateExpressionDepth(expression);

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
            String errorDetails = errorListener.getErrorMessages().stream()
                    .map(error -> "- " + error)
                    .collect(Collectors.joining("\n"));

            throw new WorkflowInvalidStateException("workflowExpressionInvalid", expression, errorDetails);
        }
        return context;
    }

    /**
     * Validates that the nesting depth of parenthesized groups does not exceed {@link #MAX_EXPRESSION_DEPTH}.
     * Runs before ANTLR parsing to avoid building a deep parse tree that could cause a {@code StackOverflowError}.
     * Condition call parentheses (e.g. {@code has-role(admin)}) are skipped since they don't produce recursive
     * grammar rules.
     */
    private static void validateExpressionDepth(String expression) {
        int depth = 0;
        int maxDepth = 0;
        boolean wasIdentChar = false;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            // condition call parens, e.g. has-role(admin) — preceded by identifier chars, skip to closing ')'
            if (c == '(' && wasIdentChar) {
                for (i++; i < expression.length(); i++) {
                    if (expression.charAt(i) == '\\' && i + 1 < expression.length()) {
                        i++; // skip escaped character
                        continue;
                    }
                    if (expression.charAt(i) == ')') {
                        break;
                    }
                }
                wasIdentChar = false;
                continue;
            }
            // grouping parens — not preceded by identifier chars, count as nesting
            if (c == '(') {
                maxDepth = Math.max(maxDepth, ++depth);
                wasIdentChar = false;
            } else if (c == ')') {
                depth = Math.max(0, depth - 1);
                wasIdentChar = false;
            } else {
                // track whether the next '(' would be a condition call or a grouping paren
                wasIdentChar = Character.isLetterOrDigit(c) || c == '-' || c == '_';
            }
        }
        if (maxDepth > MAX_EXPRESSION_DEPTH) {
            throw new WorkflowInvalidStateException("workflowExpressionMaxDepth", MAX_EXPRESSION_DEPTH);
        }
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
