package org.keycloak.tests.broker.oidc;

import java.util.Locale;

import org.keycloak.OAuth2Constants;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public class KcOidcBrokerUiLocalesWithIdpHintTest extends AbstractKcOidcBrokerTest {

    private static final Locale HUNGARIAN = Locale.forLanguageTag("hu");

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = I18nProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = I18nUiLocalesConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @Override
    protected void loginUser() {
        oauth.loginForm()
                .uiLocales(HUNGARIAN.toLanguageTag())
                .param("kc_idp_hint", IDP_OIDC_ALIAS)
                .open();

        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
        assertThat(OAuth2Constants.UI_LOCALES_PARAM + "=" + HUNGARIAN.toLanguageTag() + " should be part of the url",
                webDriver.getCurrentUrl(), containsString(OAuth2Constants.UI_LOCALES_PARAM + "=" + HUNGARIAN.toLanguageTag()));
        // The provider login page must render in Hungarian ("Sign in to your account").
        assertThat(webDriver.driver().getPageSource(), containsString("Jelentkezzen be a fiókjába"));

        logInAsUserInIDPForFirstTime();

        // The consumer first-broker-login review-profile page must also render in Hungarian
        // ("Update Account Information"). It shows because the provider user has no last name,
        // so the imported consumer user is incomplete and the review page is required.
        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/"));
        assertThat(webDriver.driver().getPageSource(), containsString("Fiók adatainak módosítása"));

        updateAccountInformation();
        assertUserCreatedInConsumerRealm();
    }

    static class I18nProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // Deliberately omit the last name so the imported consumer user is incomplete and the
            // first-broker-login review-profile page is shown (mirroring the legacy test), letting us
            // assert the consumer renders it in Hungarian.
            UserBuilder user = UserBuilder.create(USER_LOGIN)
                    .email(USER_EMAIL)
                    .emailVerified(true)
                    .password(USER_PASSWORD)
                    .enabled(true)
                    .firstName("First");
            return realm.name(PROVIDER_REALM)
                    .eventsListeners("jboss-logging")
                    .users(user)
                    .clients(createDefaultProviderClient())
                    .internationalizationEnabled(true)
                    .supportedLocales("en", "hu")
                    .defaultLocale("en");
        }
    }

    static class I18nUiLocalesConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute("uiLocales", "true"))
                    .internationalizationEnabled(true)
                    .supportedLocales("en", "hu")
                    .defaultLocale("en");
        }
    }
}
