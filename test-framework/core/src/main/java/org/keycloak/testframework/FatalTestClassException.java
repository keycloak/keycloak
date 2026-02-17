package org.keycloak.testframework;

public class FatalTestClassException extends RuntimeException {

    public FatalTestClassException(String message) {
        super(message);
    }

}
