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
        log.infof("SESSION FAILOVER TEST: cluster size = %d, session-cache owners = %d", getClusterSize(), SESSION_CACHE_OWNERS);

        assertEquals(2, getClusterSize());
        runSessionFailoverFlow();
    }

    private void runSessionFailoverFlow() {
        Cookie sessionCookie = login();

        switchFailedNode();
        sessionCookie = verifyLoggedInOrRelogin(sessionCookie);

        switchFailedNode();
        sessionCookie = verifyLoggedInOrRelogin(sessionCookie);

        logout();
        verifyLoggedOut();

        switchFailedNode();
        verifyLoggedOut();
    }

    private Cookie verifyLoggedInOrRelogin(Cookie expectedSessionCookie) {
        try {
            return verifyLoggedIn(expectedSessionCookie);
        } catch (AssertionError e) {
            return login();
        }
    }
}
