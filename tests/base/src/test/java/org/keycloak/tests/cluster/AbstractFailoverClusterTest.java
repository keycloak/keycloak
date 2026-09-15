package org.keycloak.tests.cluster;

import org.keycloak.models.UserModel;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LogoutConfirmPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.Cookie;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public abstract class AbstractFailoverClusterTest extends AbstractClusterTest {

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    protected ManagedWebDriver driver;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    protected OAuthClient oauth;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected LogoutConfirmPage logoutConfirmPage;

    @InjectPage
    protected InfoPage infoPage;

    public static final String KEYCLOAK_SESSION_COOKIE = "KEYCLOAK_SESSION";

    public static final Integer SESSION_CACHE_OWNERS = Integer.parseInt(System.getProperty("session.cache.owners", "1"));
    public static final Integer OFFLINE_SESSION_CACHE_OWNERS = Integer.parseInt(System.getProperty("offline.session.cache.owners", "1"));
    public static final Integer LOGIN_FAILURES_CACHE_OWNERS = Integer.parseInt(System.getProperty("login.failure.cache.owners", "1"));

    public static final Integer REBALANCE_WAIT = Integer.parseInt(System.getProperty("rebalance.wait", "5000"));

    @BeforeEach
    public void setupFailoverRealmUsers() {
        managedRealm.addUser(UserBuilder.create("session-user")
                .email("session-user@localhost")
                .firstName("Session")
                .lastName("User")
                .emailVerified(true)
                .password("password")
                .realmRoles("user"));

        managedRealm.addUser(UserBuilder.create("login-test")
                .email("login@test.com")
                .password("password")
                .requiredActions(UserModel.RequiredAction.UPDATE_PASSWORD.toString(),
                        UserModel.RequiredAction.UPDATE_PROFILE.toString()));
    }

    protected void switchFailedNode() {
        failback();
        pause(REBALANCE_WAIT);

        iterateCurrentFailNode();
        failure();
        pause(REBALANCE_WAIT);
    }

    protected Cookie login() {
        oauth.openLoginForm();
        if (isOnLoginPage()) {
            loginPage.fillLogin("session-user", "password");
            loginPage.submit();
        }
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        driver.driver().navigate().to(oauth.getBaseUrl() + "/realms/" + managedRealm.getName() + "/");
        Cookie sessionCookie = driver.driver().manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookie);
        return sessionCookie;
    }

    protected void logout() {
        oauth.openLogoutForm();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();

        infoPage.assertCurrent();
    }

    protected Cookie verifyLoggedIn(Cookie expectedSessionCookie) {
        driver.driver().navigate().to(oauth.getBaseUrl() + "/realms/" + managedRealm.getName() + "/");
        Cookie realmPathSessionCookie = driver.driver().manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(realmPathSessionCookie);
        Assertions.assertEquals(realmPathSessionCookie.getValue(), expectedSessionCookie.getValue());

        driver.driver().navigate().to(oauth.getRedirectUri());
        Cookie sessionCookie = driver.driver().manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookie);
        Assertions.assertEquals(sessionCookie.getValue(), expectedSessionCookie.getValue());
        return sessionCookie;
    }

    protected void verifyLoggedOut() {
        oauth.openLoginForm();
        driver.driver().navigate().refresh();
        loginPage.assertCurrent();
        Cookie sessionCookie = driver.driver().manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNull(sessionCookie);
    }

    protected boolean isOnLoginPage() {
        try {
            loginPage.assertCurrent();
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }
}
