package org.keycloak.tests.broker.oidc;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import org.junit.jupiter.api.BeforeEach;

@KeycloakIntegrationTest
public class KcOidcBrokerPrivateKeyJwtCustomAudienceTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = KcOidcBrokerPrivateKeyJwtTest.PrivateKeyJwtProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = CustomAudienceConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @BeforeEach
    void configureJwksUrl() {
        configureProviderClientJwksUrl(CLIENT_ID);
    }

    // The client assertion audience must match the provider's actual base URL (its token endpoint
    // issuer), which is only known once the provider realm exists.
    @BeforeEach
    void configureClientAssertionAudience() {
        IdentityProviderRepresentation idp = consumerRealm.admin()
                .identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put("clientAssertionAudience", providerRealm.getBaseUrl());
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }

    static class CustomAudienceConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute("clientSecret", null)
                            .attribute("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT));
        }
    }
}
