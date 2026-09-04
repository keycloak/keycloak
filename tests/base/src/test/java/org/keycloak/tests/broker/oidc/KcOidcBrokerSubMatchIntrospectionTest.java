package org.keycloak.tests.broker.oidc;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class KcOidcBrokerSubMatchIntrospectionTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = SubOverrideProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectPage
    ErrorPage errorPage;

    @Override
    @Test
    public void testLogInAsUserInIDP() {
        oauth.openLoginForm();
        logInWithBroker();
        logInAsUserInIDPForFirstTime();
        errorPage.assertCurrent();
    }

    @Override
    @Test
    @Disabled("Sub mismatch prevents login flow from completing")
    public void testLoginWithExistingUser() {
    }

    static class SubOverrideProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(PROVIDER_REALM)
                    .eventsListeners("jboss-logging")
                    .users(UserBuilder.create(USER_LOGIN)
                            .email(USER_EMAIL)
                            .emailVerified(true)
                            .firstName("First")
                            .lastName("Last")
                            .password(USER_PASSWORD)
                            .enabled(true))
                    .clients(createDefaultProviderClient()
                            .protocolMappers(
                                    ProtocolMapperBuilder.create().name("sub-override")
                                            .protocolMapper(HardcodedClaim.PROVIDER_ID)
                                            .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                                            .config(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "sub")
                                            .config("claim.value", "overridden")
                                            .config(OIDCAttributeMapperHelper.JSON_TYPE, "String")
                                            .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "false")
                                            .config(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "false")
                                            .config(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true")
                                            .build()));
        }
    }
}
