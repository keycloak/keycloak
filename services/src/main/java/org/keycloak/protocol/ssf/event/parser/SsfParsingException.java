package org.keycloak.protocol.ssf.event.parser;

import org.keycloak.protocol.ssf.SsfException;

public class SsfParsingException extends SsfException {

    public SsfParsingException(String message) {
        super(message);
    }

    public SsfParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
