package org.keycloak.federation.sssd.api;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 */
public class SSSDException extends RuntimeException {
    public SSSDException() {
    }

    public SSSDException(String message) {
        super(message);
    }

    public SSSDException(String message, Throwable cause) {
        super(message, cause);
    }
}
