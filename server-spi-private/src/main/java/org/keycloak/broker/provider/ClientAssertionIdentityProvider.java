package org.keycloak.broker.provider;

import org.keycloak.authentication.ClientAuthenticationFlowContext;

public interface ClientAssertionIdentityProvider {

    boolean verifyClientAssertion(ClientAuthenticationFlowContext context) throws Exception;

}
