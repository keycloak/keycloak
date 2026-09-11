package org.keycloak.tests.cluster;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class SessionFailoverClusterTest extends AbstractFailoverClusterTest {

    @BeforeEach
    public void beforeSessionFailover() {
        log.info("Initial node failure");
        failure();
        pause(REBALANCE_WAIT);
    }

    @Test
    public void sessionFailover() {
        boolean expectSuccessfulFailover = SESSION_CACHE_OWNERS >= 2;

        log.infof("SESSION FAILOVER TEST: cluster size = %d, session-cache owners = %d --> testing for %sSUCCESSFUL session failover",
                getClusterSize(), SESSION_CACHE_OWNERS, expectSuccessfulFailover ? "" : "UN");

        assertEquals(2, getClusterSize());
        runSessionFailoverFlow(expectSuccessfulFailover);
    }

    private void runSessionFailoverFlow(boolean expectSuccessfulFailover) {
        Cookie sessionCookie = login();

        switchFailedNode();
        if (expectSuccessfulFailover) {
            verifyLoggedIn(sessionCookie);
        } else {
            verifyLoggedOut();
        }

        switchFailedNode();
        if (expectSuccessfulFailover) {
            verifyLoggedIn(sessionCookie);
        } else {
            verifyLoggedOut();
            login();
        }

        logout();
        verifyLoggedOut();

        switchFailedNode();
        verifyLoggedOut();
    }
}
