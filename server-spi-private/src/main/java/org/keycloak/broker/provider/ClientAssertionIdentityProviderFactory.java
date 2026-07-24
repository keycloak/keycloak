package org.keycloak.broker.provider;

import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;

public interface ClientAssertionIdentityProviderFactory {

    default ClientAssertionStrategy getClientAssertionStrategy() {
        return null;
    }

    interface ClientAssertionStrategy {

        boolean isSupportedAssertionType(String assertionType);

        LookupResult lookup(ClientAuthenticationFlowContext context) throws Exception;

    }

    record LookupResult(ClientModel clientModel, IdentityProviderModel identityProviderModel) {}

}
