package org.keycloak.tests.cluster;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        boolean expectSuccessfulFailover = SESSION_CACHE_OWNERS >= 2;

        log.infof("AUTHENTICATION FAILOVER TEST: cluster size = %d, session-cache owners = %d --> testing for %sSUCCESSFUL session failover",
                getClusterSize(), SESSION_CACHE_OWNERS, expectSuccessfulFailover ? "" : "UN");

        assertEquals(2, getClusterSize());
        failoverTest(expectSuccessfulFailover);
    }

    private void failoverTest(boolean expectSuccessfulFailover) {
        oauth.openLoginForm();

        String cookieValue1 = ClusterTestUtils.getAuthSessionCookieValue(driver.driver());

        loginPage.fillLogin("login-test", "password");
        loginPage.submit();
        updateProfilePage.assertCurrent();

        Assertions.assertEquals(cookieValue1, ClusterTestUtils.getAuthSessionCookieValue(driver.driver()));
        setCurrentFailNodeForRoute(cookieValue1);

        failure();
        pause(REBALANCE_WAIT);
        logFailoverSetup();

        updateProfilePage.prepareUpdate().firstName("John").lastName("Doe3").email("john@doe3.com").submit();

        if (expectSuccessfulFailover) {
            updatePasswordPage.assertCurrent();

            String cookieValue2 = ClusterTestUtils.getAuthSessionCookieValue(driver.driver());
            assertNotNull(cookieValue2);
            Assertions.assertEquals(cookieValue1.substring(0, 36), cookieValue2.substring(0, 36));
            assertNotEquals(cookieValue1, cookieValue2);
        } else {
            loginPage.assertCurrent();
            assertNotNull(loginPage.getErrorMessage().orElse(null));

            loginPage.fillLogin("login-test", "password");
            loginPage.submit();
            updateProfilePage.prepareUpdate().firstName("John").lastName("Doe3").email("john@doe3.com").submit();
        }

        updatePasswordPage.assertCurrent();
        updatePasswordPage.changePassword("password", "password");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }
}
