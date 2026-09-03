package org.keycloak.tests.broker;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;

@KeycloakIntegrationTest
public class KcOidcBrokerClientSecretBasicAuthTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = ConsumerRealmWithBasicAuth.class)
    ManagedRealm consumerRealm;

    static class ConsumerRealmWithBasicAuth implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_BASIC));
        }
    }
}
