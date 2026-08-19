package org.keycloak.tests.broker;

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
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public class KcOidcBrokerUiLocalesWithIdpHintTest implements BrokerLoginTest, OidcBrokerConfigSupport {

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

        logInAsUserInIDPForFirstTime();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();
    }

    static class I18nProviderRealmConfig extends OidcBrokerConfigSupport.OidcProviderRealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return super.configure(realm)
                    .internationalizationEnabled(true)
                    .supportedLocales("en", "hu")
                    .defaultLocale("en");
        }
    }

    static class I18nUiLocalesConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return OidcBrokerConfigSupport.configureConsumerRealm(realm,
                    OidcBrokerConfigSupport.createOidcIdentityProvider()
                            .attribute("uiLocales", "true"))
                    .internationalizationEnabled(true)
                    .supportedLocales("en", "hu")
                    .defaultLocale("en");
        }
    }
}
