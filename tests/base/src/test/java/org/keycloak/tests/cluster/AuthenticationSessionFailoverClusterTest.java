package org.keycloak.tests.cluster;

import org.keycloak.cookie.CookieType;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class AuthenticationSessionFailoverClusterTest extends AbstractFailoverClusterTest {

    @InjectPage
    protected LoginPasswordUpdatePage updatePasswordPage;

    @InjectPage
    protected LoginUpdateProfilePage updateProfilePage;

    @Test
    public void failoverDuringAuthentication() {
        log.infof("AUTHENTICATION FAILOVER TEST: cluster size = %d, session-cache owners = %d",
                getClusterSize(), SESSION_CACHE_OWNERS);

        assertEquals(2, getClusterSize());
        failoverTest();
    }

    private void failoverTest() {
        oauth.openLoginForm();

        String cookieValue1 = getAuthSessionCookieValue(driver.driver());

        loginPage.fillLogin("login-test", "password");
        loginPage.submit();
        updateProfilePage.assertCurrent();

        Assertions.assertEquals(cookieValue1, getAuthSessionCookieValue(driver.driver()));
        if (cookieValue1.contains("node")) {
            setCurrentFailNodeForRoute(cookieValue1);
        }

        failure();
        pause(REBALANCE_WAIT);
        logFailoverSetup();

        updateProfilePage.prepareUpdate().firstName("John").lastName("Doe3").email("john@doe3.com").submit();

        if (isOnLoginPage()) {
            assertNotNull(loginPage.getErrorMessage().orElse(null));

            loginPage.fillLogin("login-test", "password");
            loginPage.submit();
            updateProfilePage.prepareUpdate().firstName("John").lastName("Doe3").email("john@doe3.com").submit();
            updatePasswordPage.assertCurrent();
        } else {
            updatePasswordPage.assertCurrent();

            String cookieValue2 = getAuthSessionCookieValue(driver.driver());
            assertNotNull(cookieValue2);
            assertNotEquals("", cookieValue2);
        }

        updatePasswordPage.assertCurrent();
        updatePasswordPage.changePassword("password", "password");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    public static String getAuthSessionCookieValue(WebDriver driver) {
        Cookie authSessionCookie = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        assertNotNull(authSessionCookie);
        return authSessionCookie.getValue();
    }
}
