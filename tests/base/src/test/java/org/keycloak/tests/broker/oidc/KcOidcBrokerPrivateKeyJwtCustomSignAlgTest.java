package org.keycloak.tests.broker.oidc;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import org.junit.jupiter.api.BeforeEach;

@KeycloakIntegrationTest
public class KcOidcBrokerPrivateKeyJwtCustomSignAlgTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = EcdsaConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = EcdsaProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @BeforeEach
    void addEcdsaKeyProvider() {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("ecdsa-es256");
        rep.setParentId(consumerRealm.admin().toRepresentation().getId());
        rep.setProviderId(GeneratedEcdsaKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(System.currentTimeMillis()));
        rep.getConfig().putSingle("active", "true");
        rep.getConfig().putSingle("enabled", "true");
        rep.getConfig().putSingle(GeneratedEcdsaKeyProviderFactory.ECDSA_ELLIPTIC_CURVE_KEY, "P-256");
        try (Response response = consumerRealm.admin().components().add(rep)) {
            String id = CreatedResponseUtil.getCreatedId(response);
            consumerRealm.cleanup().add(r -> r.components().component(id).remove());
        }
    }

    @BeforeEach
    void configureJwksUrl() {
        configureProviderClientJwksUrl(CLIENT_ID);
    }

    static class EcdsaProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureProviderRealm(realm,
                    createDefaultProviderClient()
                            .authenticatorType(JWTClientAuthenticator.PROVIDER_ID)
                            .attribute(OIDCConfigAttributes.USE_JWKS_URL, "true")
                            .attribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, Algorithm.ES256));
        }
    }

    static class EcdsaConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute("clientSecret", null)
                            .attribute("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT)
                            .attribute("clientAssertionSigningAlg", Algorithm.ES256));
        }
    }
}
