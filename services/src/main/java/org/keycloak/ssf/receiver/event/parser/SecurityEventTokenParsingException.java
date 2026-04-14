package org.keycloak.ssf.receiver.event.parser;

import org.keycloak.ssf.SsfException;

public class SecurityEventTokenParsingException extends SsfException {

    public SecurityEventTokenParsingException(String message) {
        super(message);
    }

    public SecurityEventTokenParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
