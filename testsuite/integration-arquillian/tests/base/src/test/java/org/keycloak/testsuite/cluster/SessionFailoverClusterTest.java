package org.keycloak.testsuite.cluster;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import org.openqa.selenium.Cookie;

/**
 *
 * @author tkyjovsk
 */
public class SessionFailoverClusterTest extends AbstractTwoNodeClusterTest {

    public static final String KEYCLOAK_SESSION_COOKIE = "KEYCLOAK_SESSION";
    public static final String KEYCLOAK_IDENTITY_COOKIE = "KEYCLOAK_IDENTITY";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    @Ignore("work in progress") // only works with owners="2" at the moment
    public void sessionFailover() {
        
        // LOGOUT
        accountPage.navigateTo();
        driver.navigate().refresh();
        pause(3000);
        loginPage.form().login(ADMIN, ADMIN);
        assertCurrentUrlStartsWith(accountPage);
        
        Cookie sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookie);

        killBackend1();

        // check if session survived backend failure
        
        driver.navigate().refresh();
        pause(3000);
        
        assertCurrentUrlStartsWith(accountPage);
        Cookie sessionCookieAfterFailover = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookieAfterFailover);
        assertEquals(sessionCookieAfterFailover.getValue(), sessionCookie.getValue());

        failback();

        // check if session survived backend failback
        driver.navigate().refresh();
        pause(3000);
        assertCurrentUrlStartsWith(accountPage);
        Cookie sessionCookieAfterFailback = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNotNull(sessionCookieAfterFailback);
        assertEquals(sessionCookieAfterFailover.getValue(), sessionCookie.getValue());

        // LOGOUT
        accountPage.navigateTo();
        accountPage.signOut();

        assertCurrentUrlDoesntStartWith(accountPage);
        masterRealmPage.navigateTo();
        sessionCookie = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNull(sessionCookie);

        killBackend1();
        
        // check if session survived backend failure
        driver.navigate().refresh();
        pause(3000);
        assertCurrentUrlDoesntStartWith(accountPage);
        masterRealmPage.navigateTo();
        sessionCookieAfterFailover = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNull(sessionCookieAfterFailover);
        
        failback();
    
        // check if session survived backend failback
        driver.navigate().refresh();
        pause(3000);
        assertCurrentUrlDoesntStartWith(accountPage);
        masterRealmPage.navigateTo();
        sessionCookieAfterFailback = driver.manage().getCookieNamed(KEYCLOAK_SESSION_COOKIE);
        assertNull(sessionCookieAfterFailback);
    }

}
