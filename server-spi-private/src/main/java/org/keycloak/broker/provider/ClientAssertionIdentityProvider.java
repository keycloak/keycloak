package org.keycloak.broker.provider;

import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.models.IdentityProviderModel;

public interface ClientAssertionIdentityProvider<C extends IdentityProviderModel> extends IdentityProvider<C> {

    boolean verifyClientAssertion(ClientAuthenticationFlowContext context) throws Exception;

}
