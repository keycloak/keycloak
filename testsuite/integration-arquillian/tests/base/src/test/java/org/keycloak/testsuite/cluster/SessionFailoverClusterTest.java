package org.keycloak.testsuite.cluster;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Cookie;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 *
 * @author tkyjovsk
 */
public class SessionFailoverClusterTest extends AbstractFailoverClusterTest {

    @Before
    public void beforeSessionFailover() {
        log.info("Initial node failure");
        failure();
        pause(REBALANCE_WAIT);
    }

    @Test
    public void sessionFailover() {

        boolean expectSuccessfulFailover = SESSION_CACHE_OWNERS >= 2;

        log.info("SESSION FAILOVER TEST: cluster size = " + getClusterSize() + ", session-cache owners = " + SESSION_CACHE_OWNERS
                + " --> Testsing for " + (expectSuccessfulFailover ? "" : "UN") + "SUCCESSFUL session failover.");

        assertEquals(2, getClusterSize());
        
        sessionFailover(expectSuccessfulFailover);
    }

    protected void sessionFailover(boolean expectSuccessfulFailover) {

        // LOGIN
        Cookie sessionCookie = login(accountPage);

        switchFailedNode();

        // VERIFY
        if (expectSuccessfulFailover) {
            verifyLoggedIn(accountPage, sessionCookie);
        } else {
            verifyLoggedOut(accountPage);
            // FIXME test fails if I put re-login here
        }

        switchFailedNode();

        // VERIFY again
        if (expectSuccessfulFailover) {
            verifyLoggedIn(accountPage, sessionCookie);
        } else {
            verifyLoggedOut(accountPage);
            login(accountPage);
        }

        // LOGOUT
        logout(accountPage);
        verifyLoggedOut(accountPage);

        switchFailedNode();

        // VERIFY
        verifyLoggedOut(accountPage);

    }

}
