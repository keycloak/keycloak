package org.keycloak.tests.broker.oidc;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

@KeycloakIntegrationTest
public class KcOidcBrokerPkceTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = PkceConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    static class PkceConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute(OAuth2IdentityProviderConfig.PKCE_ENABLED, "true")
                            .attribute(OAuth2IdentityProviderConfig.PKCE_METHOD, OAuth2Constants.PKCE_METHOD_S256));
        }
    }
}
