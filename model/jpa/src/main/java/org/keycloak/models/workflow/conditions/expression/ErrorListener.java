package org.keycloak.models.workflow.conditions.expression;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class ErrorListener extends BaseErrorListener {
    private boolean hasErrors = false;
    private final List<String> errorMessages = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg, RecognitionException e) {
        hasErrors = true;
        String error = String.format("Error at line %d:%d - %s", line, charPositionInLine, msg);
        errorMessages.add(error);
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }
}
