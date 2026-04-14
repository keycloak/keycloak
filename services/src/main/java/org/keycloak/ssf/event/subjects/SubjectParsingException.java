package org.keycloak.ssf.event.subjects;

import org.keycloak.ssf.SsfException;

public class SubjectParsingException extends SsfException {

    public SubjectParsingException() {
    }

    public SubjectParsingException(String message) {
        super(message);
    }

    public SubjectParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
