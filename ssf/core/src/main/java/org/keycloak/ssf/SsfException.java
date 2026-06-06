package org.keycloak.ssf;

import org.keycloak.models.ModelException;

public class SsfException extends ModelException {

    public SsfException() {
    }

    public SsfException(String message) {
        super(message);
    }

    public SsfException(String message, Throwable cause) {
        super(message, cause);
    }
}
