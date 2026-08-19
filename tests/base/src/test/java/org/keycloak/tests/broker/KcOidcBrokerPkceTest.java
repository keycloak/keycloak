package org.keycloak.tests.broker;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

@KeycloakIntegrationTest
public class KcOidcBrokerPkceTest extends AbstractBrokerTest {

    @InjectRealm
    ManagedRealm managedRealm;

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
