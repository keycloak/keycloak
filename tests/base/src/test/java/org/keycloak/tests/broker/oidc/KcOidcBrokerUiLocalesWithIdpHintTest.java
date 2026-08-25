package org.keycloak.tests.broker.oidc;

import java.util.Locale;

import org.keycloak.OAuth2Constants;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.broker.BrokerLoginTest;
import org.keycloak.tests.broker.KcOidcBrokerConfigSupport;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public class KcOidcBrokerUiLocalesWithIdpHintTest implements BrokerLoginTest, KcOidcBrokerConfigSupport {

    private static final Locale HUNGARIAN = Locale.forLanguageTag("hu");

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = I18nProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = I18nUiLocalesConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    IdpReviewUserProfilePage updateProfilePage;

    @Override
    public ManagedRealm getProviderRealm() {
        return providerRealm;
    }

    @Override
    public ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oauth;
    }

    @Override
    public ManagedWebDriver getWebDriver() {
        return webDriver;
    }

    @Override
    public LoginPage getLoginPage() {
        return loginPage;
    }

    @Override
    public IdpReviewUserProfilePage getUpdateProfilePage() {
        return updateProfilePage;
    }

    @Override
    public void loginUser() {
        getOAuthClient().loginForm()
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
                    .clients(KcOidcBrokerConfigSupport.createDefaultProviderClient())
                    .internationalizationEnabled(true)
                    .supportedLocales("en", "hu")
                    .defaultLocale("en");
        }
    }

    static class I18nUiLocalesConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return KcOidcBrokerConfigSupport.configureConsumerRealm(realm,
                    KcOidcBrokerConfigSupport.createOidcIdentityProvider()
                            .attribute("uiLocales", "true"))
                    .internationalizationEnabled(true)
                    .supportedLocales("en", "hu")
                    .defaultLocale("en");
        }
    }
}
