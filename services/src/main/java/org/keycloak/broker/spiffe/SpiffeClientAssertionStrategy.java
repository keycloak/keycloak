package org.keycloak.broker.spiffe;

import java.util.Map;

import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.authentication.authenticators.client.ClientAssertionState;
import org.keycloak.authentication.authenticators.client.FederatedJWTClientAuthenticator;
import org.keycloak.broker.provider.ClientAssertionIdentityProviderFactory;
import org.keycloak.cache.AlternativeLookupProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;

public class SpiffeClientAssertionStrategy implements ClientAssertionIdentityProviderFactory.ClientAssertionStrategy {

    @Override
    public boolean isSupportedAssertionType(String assertionType) {
        return SpiffeConstants.CLIENT_ASSERTION_TYPE.equals(assertionType);
    }

    @Override
    public ClientAssertionIdentityProviderFactory.LookupResult lookup(ClientAuthenticationFlowContext context) throws Exception {
        ClientAssertionState clientAssertionState = context.getState(ClientAssertionState.class, ClientAssertionState.supplier());
        AlternativeLookupProvider lookupProvider = context.getSession().getProvider(AlternativeLookupProvider.class);

        String federatedClientId =  clientAssertionState.getToken().getSubject();

        ClientModel client = lookupProvider.lookupClientFromClientAttributes(
                context.getSession(),
                Map.of(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_SUBJECT_KEY, federatedClientId));
        IdentityProviderModel identityProvider = context.getSession().identityProviders().getByAlias(
                client.getAttribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_ISSUER_KEY));

        return new ClientAssertionIdentityProviderFactory.LookupResult(client, identityProvider);
    }

}
