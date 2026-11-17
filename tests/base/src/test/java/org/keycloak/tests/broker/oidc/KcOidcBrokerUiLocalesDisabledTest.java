package org.keycloak.tests.broker.oidc;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import static java.util.Locale.ENGLISH;

import static org.keycloak.OAuth2Constants.UI_LOCALES_PARAM;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public class KcOidcBrokerUiLocalesDisabledTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = UiLocalesDisabledConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @Override
    protected void loginUser() {
        oauth.openLoginForm();

        logInWithBroker();

        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
        assertThat(UI_LOCALES_PARAM + " should NOT be part of the url",
                webDriver.getCurrentUrl(), not(containsString(UI_LOCALES_PARAM + "=" + ENGLISH.toLanguageTag())));

        logInAsUserInIDPForFirstTime();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();
    }

    static class UiLocalesDisabledConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute("uiLocales", "false"));
        }
    }
}
