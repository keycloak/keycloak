package org.keycloak.tests.sessionlimits;

import org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.ERROR_TO_DISPLAY;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.assertSessionCount;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.configurePostBrokerFlow;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.deleteAllCookies;

@KeycloakIntegrationTest
public abstract class AbstractUserSessionLimitsBrokerTest {

    protected static final String PROVIDER_REALM = "provider";
    protected static final String CONSUMER_REALM = "consumer";
    protected static final String USER_LOGIN = "testuser";
    protected static final String USER_PASSWORD = "password";
    protected static final String USER_EMAIL = "user@localhost.com";
    protected static final String CONSUMER_CLIENT_ID = "broker-app";
    protected static final String CONSUMER_CLIENT_SECRET = "broker-app-secret";

    @InjectRunOnServer(realmRef = "consumer")
    RunOnServerClient runOnServer;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ErrorPage errorPage;

    protected abstract String getIdpAlias();

    protected abstract ManagedRealm getConsumerRealm();

    protected abstract ManagedRealm getProviderRealm();

    protected abstract void logInAsUserInIDPForFirstTime();

    @Test
    public void testSessionCountExceededAndNewSessionDeniedFirstBrokerLoginFlow() {
        runOnServer.run(configurePostBrokerFlow(CONSUMER_REALM, getIdpAlias(),
                UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1"));

        logInAsUserInIDPForFirstTime();
        deleteAllCookies(driver, getConsumerRealm());
        deleteAllCookies(driver, getProviderRealm());
        logInAsUserInIDP();

        errorPage.assertCurrent();
        Assertions.assertEquals(ERROR_TO_DISPLAY, errorPage.getError());
    }

    @Test
    public void testSessionCountExceededAndOldestSessionRemovedFirstBrokerLoginFlow() {
        runOnServer.run(configurePostBrokerFlow(CONSUMER_REALM, getIdpAlias(),
                UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION, "0", "1"));

        logInAsUserInIDPForFirstTime();
        deleteAllCookies(driver, getConsumerRealm());
        deleteAllCookies(driver, getProviderRealm());
        logInAsUserInIDP();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        runOnServer.run(assertSessionCount(CONSUMER_REALM, USER_LOGIN, 1));
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedFirstBrokerLoginFlow() {
        runOnServer.run(configurePostBrokerFlow(CONSUMER_REALM, getIdpAlias(),
                UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "1", "0"));

        logInAsUserInIDPForFirstTime();
        deleteAllCookies(driver, getConsumerRealm());
        deleteAllCookies(driver, getProviderRealm());
        logInAsUserInIDP();

        errorPage.assertCurrent();
        Assertions.assertEquals(ERROR_TO_DISPLAY, errorPage.getError());
    }

    @Test
    public void testRealmSessionCountExceededAndOldestSessionRemovedFirstBrokerLoginFlow() {
        runOnServer.run(configurePostBrokerFlow(CONSUMER_REALM, getIdpAlias(),
                UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION, "1", "0"));

        logInAsUserInIDPForFirstTime();
        deleteAllCookies(driver, getConsumerRealm());
        deleteAllCookies(driver, getProviderRealm());
        logInAsUserInIDP();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        runOnServer.run(assertSessionCount(CONSUMER_REALM, USER_LOGIN, 1));
    }

    protected void logInAsUserInIDP() {
        oauth.realm(CONSUMER_REALM).client(CONSUMER_CLIENT_ID, CONSUMER_CLIENT_SECRET).openLoginForm();
        loginPage.assertCurrent();
        loginPage.clickSocial(getIdpAlias());
        loginPage.assertCurrent();
        loginPage.fillLogin(USER_LOGIN, USER_PASSWORD);
        loginPage.submit();
    }
}
