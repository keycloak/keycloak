package org.keycloak.client.registration;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationException extends Exception {

    public ClientRegistrationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ClientRegistrationException(String s) {
        super(s);
    }

}
