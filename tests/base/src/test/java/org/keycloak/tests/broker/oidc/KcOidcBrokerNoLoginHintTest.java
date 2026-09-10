package org.keycloak.tests.broker.oidc;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import org.junit.jupiter.api.Assertions;

@KeycloakIntegrationTest
public class KcOidcBrokerNoLoginHintTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = NoLoginHintConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @Override
    protected void loginUser() {
        oauth.loginForm().loginHint(USER_EMAIL).open();

        logInWithBroker();

        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"),
                "Driver should be on the provider realm page right now");
        Assertions.assertTrue(loginPage.getUsername().isBlank(),
                "User identifier should not be filled");

        logInAsUserInIDPForFirstTime();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();
    }

    static class NoLoginHintConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute(IdentityProviderModel.LOGIN_HINT, "false"));
        }
    }
}
