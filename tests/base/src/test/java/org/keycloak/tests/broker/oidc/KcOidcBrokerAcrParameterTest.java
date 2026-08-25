package org.keycloak.tests.broker.oidc;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
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
public class KcOidcBrokerAcrParameterTest implements BrokerLoginTest, KcOidcBrokerConfigSupport {

    private static final String ACR_VALUES = "acr_values";
    private static final String ACR_3 = "3";

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = KcOidcBrokerConfigSupport.OidcProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = KcOidcBrokerConfigSupport.OidcConsumerRealmConfig.class)
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
        getOAuthClient().loginForm().param(ACR_VALUES, ACR_3).open();

        logInWithBroker();

        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
        assertThat(ACR_VALUES + "=" + ACR_3 + " should be part of the url",
                webDriver.getCurrentUrl(), containsString(ACR_VALUES + "=" + ACR_3));

        logInAsUserInIDPForFirstTime();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();
    }
}
