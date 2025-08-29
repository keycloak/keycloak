package org.keycloak.broker.provider;

public interface ClientAssertionIdentityProvider {

    boolean verifyClientAssertion(ClientAssertionContext context);

}
