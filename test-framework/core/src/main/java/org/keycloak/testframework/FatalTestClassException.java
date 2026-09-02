package org.keycloak.testframework;

/**
 * FatalTestClassException is thrown when a test class contains invalid configuration, or there is a non-recoverable
 * problem when setting up managed resources for a test, for example the server can not be started. When a
 * FatalTestClassException is thrown subsequent test methods in a test class will be skipped.
 */
public class FatalTestClassException extends RuntimeException {

    public FatalTestClassException(String message) {
        super(message);
    }

}
