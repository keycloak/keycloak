package org.keycloak.testsuite.broker;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

public class KcOidcBrokerPkceTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
                IdentityProviderRepresentation provider = super.setUpIdentityProvider(syncMode);

                provider.getConfig().put(OAuth2IdentityProviderConfig.PKCE_ENABLED, "true");
                provider.getConfig().put(OAuth2IdentityProviderConfig.PKCE_METHOD, OAuth2Constants.PKCE_METHOD_S256);

                return provider;
            }
        };
    }
}
