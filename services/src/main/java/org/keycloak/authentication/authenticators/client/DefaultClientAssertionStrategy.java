package org.keycloak.authentication.authenticators.client;

import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.broker.provider.ClientAssertionIdentityProviderFactory;
import org.keycloak.cache.AlternativeLookupProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;

public class DefaultClientAssertionStrategy implements ClientAssertionIdentityProviderFactory.ClientAssertionStrategy {

    @Override
    public boolean isSupportedAssertionType(String  assertionType) {
        return OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT.equals(assertionType);
    }

    @Override
    public ClientAssertionIdentityProviderFactory.LookupResult lookup(ClientAuthenticationFlowContext context) throws Exception {
        ClientAssertionState clientAssertionState = context.getState(ClientAssertionState.class, ClientAssertionState.supplier());
        AlternativeLookupProvider lookupProvider = context.getSession().getProvider(AlternativeLookupProvider.class);

        String issuer = clientAssertionState.getToken().getIssuer();
        String federatedClientId =  clientAssertionState.getToken().getSubject();

        IdentityProviderModel identityProvider = lookupProvider.lookupIdentityProviderFromIssuer(context.getSession(), issuer);
        if (identityProvider == null) {
            return null;
        }

        ClientModel client = lookupProvider.lookupClientFromClientAttributes(
                context.getSession(),
                Map.of(
                        FederatedJWTClientAuthenticator.JWT_CREDENTIAL_SUBJECT_KEY, federatedClientId,
                        FederatedJWTClientAuthenticator.JWT_CREDENTIAL_ISSUER_KEY, identityProvider.getAlias()
                )
        );

        return new ClientAssertionIdentityProviderFactory.LookupResult(client, identityProvider);
    }

}
