package org.keycloak.testsuite.cluster;

import java.util.List;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import org.openqa.selenium.Cookie;
import org.keycloak.testsuite.page.AbstractPage;
import org.keycloak.testsuite.page.PageWithLogOutAction;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author tkyjovsk
 */
public class SessionFailoverClusterTest extends AbstractClusterTest {

    public static final String KEYCLOAK_SESSION_COOKIE = "KEYCLOAK_SESSION";

    public static final Integer SESSION_CACHE_OWNERS = Integer.parseInt(System.getProperty("session.cache.owners", "1"));
    public static final Integer OFFLINE_SESSION_CACHE_OWNERS = Integer.parseInt(System.getProperty("offline.session.cache.owners", "1"));
    public static final Integer LOGIN_FAILURES_CACHE_OWNERS = Integer.parseInt(System.getProperty("login.failure.cache.owners", "1"));

    public static final Integer REBALANCE_WAIT = Integer.parseInt(System.getProperty("rebalance.wait", "5000"));

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Before
    public void beforeSessionFailover() {
        log.info("Initial node failure");
        failure();
        pause(REBALANCE_WAIT);
    }

    @Test
    public void sessionFailover() {

        boolean expectSuccessfulFailover = SESSION_CACHE_OWNERS >= getClusterSize();

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

    /**
     * failure --> failback --> failure of next node
     */
    protected void switchFailedNode() {
        assertFalse(controller.isStarted(getCurrentFailNode().getQualifier()));
        
        failback();
        pause(REBALANCE_WAIT);
        
        iterateCurrentFailNode();
        
        failure();
        pause(REBALANCE_WAIT);
        
        assertFalse(controller.isStarted(getCurrentFailNode().getQualifier()));
    }

    protected Cookie login(AbstractPage targetPage) {
        targetPage.navigateTo();
        assertCurrentUrlStartsWith(loginPage);
        loginPage.form().login(ADMIN, ADMIN);
        assertCurrentUrlStartsWith(targetPage);
        Cookie sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookie);
        return sessionCookie;
    }

    protected void logout(AbstractPage targetPage) {
        if (!(targetPage instanceof PageWithLogOutAction)) {
            throw new IllegalArgumentException(targetPage.getClass().getSimpleName() + " must implement PageWithLogOutAction interface");
        }
        targetPage.navigateTo();
        assertCurrentUrlStartsWith(targetPage);
        ((PageWithLogOutAction) targetPage).logOut();
    }

    protected Cookie verifyLoggedIn(AbstractPage targetPage, Cookie sessionCookieForVerification) {
        // verify on realm path
        masterRealmPage.navigateTo();
        Cookie sessionCookieOnRealmPath = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookieOnRealmPath);
        assertEquals(sessionCookieOnRealmPath.getValue(), sessionCookieForVerification.getValue());
        // verify on target page
        targetPage.navigateTo();
        assertCurrentUrlStartsWith(targetPage);
        Cookie sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookie);
        assertEquals(sessionCookie.getValue(), sessionCookieForVerification.getValue());
        return sessionCookie;
    }

    protected void verifyLoggedOut(AbstractPage targetPage) {
        // verify on target page
        targetPage.navigateTo();
        driver.navigate().refresh();
        assertCurrentUrlStartsWith(loginPage);
        Cookie sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNull(sessionCookie);
    }

}
