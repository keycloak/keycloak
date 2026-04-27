package org.keycloak.ssf.transmitter.stream;

import org.keycloak.ssf.SsfException;

public class DuplicateStreamConfigException extends SsfException {

    public DuplicateStreamConfigException() {
    }

    public DuplicateStreamConfigException(String message) {
        super(message);
    }

    public DuplicateStreamConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
