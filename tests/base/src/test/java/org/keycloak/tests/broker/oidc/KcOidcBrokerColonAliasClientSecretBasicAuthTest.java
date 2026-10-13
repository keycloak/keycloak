package org.keycloak.tests.broker.oidc;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

@KeycloakIntegrationTest
public class KcOidcBrokerColonAliasClientSecretBasicAuthTest extends AbstractKcOidcBrokerTest {

    static final String CLIENT_ID_COLON = "https://kc-dev.general.gr/staging/realms/general";

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = ColonAliasProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = ColonAliasConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    static class ColonAliasProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureProviderRealm(realm,
                    createDefaultProviderClient()
                            .clientId(CLIENT_ID_COLON));
        }
    }

    static class ColonAliasConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute("clientId", CLIENT_ID_COLON)
                            .attribute("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_BASIC));
        }
    }
}
