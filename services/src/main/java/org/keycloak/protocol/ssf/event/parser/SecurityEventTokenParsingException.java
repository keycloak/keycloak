package org.keycloak.protocol.ssf.event.parser;

import org.keycloak.protocol.ssf.SsfException;

public class SecurityEventTokenParsingException extends SsfException {

    public SecurityEventTokenParsingException(String message) {
        super(message);
    }

    public SecurityEventTokenParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
