package org.keycloak.tests.broker.oidc;

import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

@KeycloakIntegrationTest
public class KcOidcBrokerClientSecretJwtCustomSignAlgTest extends AbstractKcOidcBrokerTest {

    // BCFIPS approved mode requires at least 112 bits (14 characters) for client-secret-jwt
    private static final String CLIENT_SECRET_JWT = "atleast-14chars-password";

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = JwtSecretCustomAlgProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = JwtSecretCustomAlgConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    static class JwtSecretCustomAlgProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureProviderRealm(realm,
                    createDefaultProviderClient()
                            .secret(CLIENT_SECRET_JWT)
                            .authenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID)
                            .attribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, Algorithm.HS384));
        }
    }

    static class JwtSecretCustomAlgConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute("clientSecret", CLIENT_SECRET_JWT)
                            .attribute("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_JWT)
                            .attribute("clientAssertionSigningAlg", Algorithm.HS384));
        }
    }
}
