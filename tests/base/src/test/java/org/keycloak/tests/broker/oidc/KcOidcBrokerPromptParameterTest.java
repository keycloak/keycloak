package org.keycloak.tests.broker.oidc;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
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
import org.keycloak.tests.broker.BrokerLoginTest;
import org.keycloak.tests.broker.KcOidcBrokerConfigSupport;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public class KcOidcBrokerPromptParameterTest implements BrokerLoginTest, KcOidcBrokerConfigSupport {

    private static final String PROMPT_CONSENT = "consent";
    private static final String PROMPT_LOGIN = "login";

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = KcOidcBrokerConfigSupport.OidcProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = PromptConsumerRealmConfig.class)
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
                .param(OIDCLoginProtocol.PROMPT_PARAM, PROMPT_CONSENT)
                .open();

        logInWithBroker();

        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
        assertThat(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_LOGIN + " should not be part of the url",
                webDriver.getCurrentUrl(), not(containsString(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_LOGIN)));
        assertThat(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_CONSENT + " should be part of the url",
                webDriver.getCurrentUrl(), containsString(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_CONSENT));

        logInAsUserInIDPForFirstTime();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();
    }

    // The shared createOidcIdentityProvider() sets prompt=login, which the broker always prefers over the
    // client-forwarded prompt. This test needs the client's prompt=consent to pass through, so it clears the
    // IDP-configured prompt - mirroring the legacy KcOidcBrokerConfiguration2, which removed the attribute.
    static class PromptConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return KcOidcBrokerConfigSupport.configureConsumerRealm(realm,
                    KcOidcBrokerConfigSupport.createOidcIdentityProvider().attribute("prompt", null));
        }
    }
}
