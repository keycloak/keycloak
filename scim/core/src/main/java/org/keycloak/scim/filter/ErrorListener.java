package org.keycloak.scim.filter;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * ANTLR error listener that collects syntax errors during filter parsing.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ErrorListener extends BaseErrorListener {

    private boolean hasErrors = false;
    private final List<String> errorMessages = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg, RecognitionException e) {
        hasErrors = true;
        errorMessages.add(String.format("position %d: %s", charPositionInLine, msg));
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }
}
