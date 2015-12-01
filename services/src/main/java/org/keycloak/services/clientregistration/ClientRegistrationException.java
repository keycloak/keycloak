package org.keycloak.services.clientregistration;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRegistrationException extends RuntimeException {

    public ClientRegistrationException() {
        super();
    }

    public ClientRegistrationException(String message) {
        super(message);
    }

    public ClientRegistrationException(Throwable throwable) {
        super(throwable);
    }

    public ClientRegistrationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
