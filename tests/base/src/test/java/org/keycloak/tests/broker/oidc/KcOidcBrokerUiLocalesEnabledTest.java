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
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public class KcOidcBrokerUiLocalesEnabledTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = UiLocalesEnabledConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @Override
    protected void loginUser() {
        oauth.openLoginForm();

        logInWithBroker();

        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
        assertThat(UI_LOCALES_PARAM + "=" + ENGLISH.toLanguageTag() + " should be part of the url",
                webDriver.getCurrentUrl(), containsString(UI_LOCALES_PARAM + "=" + ENGLISH.toLanguageTag()));

        logInAsUserInIDPForFirstTime();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();
    }

    static class UiLocalesEnabledConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute("uiLocales", "true"));
        }
    }
}
