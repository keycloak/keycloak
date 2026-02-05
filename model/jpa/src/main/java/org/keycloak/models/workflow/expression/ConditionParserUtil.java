package org.keycloak.models.workflow.expression;

final class ConditionParserUtil {

    private ConditionParserUtil() {
        throw new UnsupportedOperationException("Static utility class");
    }

    static String extractParameter(BooleanConditionParser.ParameterContext paramCtx) {
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
    static String unEscapeParameter(String rawText) {
        // This handles both \) -> ) and \\ -> \
        // Note: replaceAll uses regex, so we must double-escape the backslashes
        return rawText.replace("\\)", ")")
                .replace("\\\\", "\\");
    }
}
