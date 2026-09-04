package org.keycloak.tests.broker.oidc;

import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import org.junit.jupiter.api.BeforeEach;

@KeycloakIntegrationTest
public class KcOidcBrokerPrivateKeyJwtTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = PrivateKeyJwtProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = PrivateKeyJwtConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @BeforeEach
    void configureJwksUrl() {
        configureProviderClientJwksUrl(CLIENT_ID);
    }

    static class PrivateKeyJwtProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureProviderRealm(realm,
                    createDefaultProviderClient()
                            .authenticatorType(JWTClientAuthenticator.PROVIDER_ID)
                            .attribute(OIDCConfigAttributes.USE_JWKS_URL, "true"));
        }
    }

    static class PrivateKeyJwtConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute("clientSecret", null)
                            .attribute("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT));
        }
    }
}
