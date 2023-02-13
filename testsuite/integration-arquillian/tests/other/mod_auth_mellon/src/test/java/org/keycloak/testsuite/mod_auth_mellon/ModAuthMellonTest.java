package org.keycloak.testsuite.mod_auth_mellon;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.util.URLAssert;
import org.keycloak.models.utils.SessionTimeoutHelper;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author mhajas
 */
public class ModAuthMellonTest extends AbstractAuthTest {
    @Page
    private ModAuthMellonProtectedResource modAuthMellonProtectedResourcePage;

    @Page
    private ModAuthMellonUnprotectedResource modAuthMellonUnprotectedResourcePage;

    @Page
    private ModAuthMellonProtectedResource2 modAuthMellonProtectedResourcePage2;

    @Page
    private ModAuthMellonUnprotectedResource2 modAuthMellonUnprotectedResourcePage2;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/mellon-realm.json"));
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm("mellon-test");
        testRealmSAMLRedirectLoginPage.setAuthRealm("mellon-test");
    }

    @Test
    public void singleLoginAndLogoutTest() {
        try {
            modAuthMellonProtectedResourcePage.navigateTo();
            URLAssert.assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);
            testRealmSAMLRedirectLoginPage.form().login(bburkeUser);
            assertTrue(driver.getPageSource().contains("Protected resource"));

            modAuthMellonProtectedResourcePage2.navigateTo();
            assertTrue(driver.getPageSource().contains("Protected resource 2"));

            modAuthMellonProtectedResourcePage2.logout();
            assertTrue(driver.getPageSource().contains("Unprotected resource 2"));

            modAuthMellonProtectedResourcePage2.navigateTo();
            URLAssert.assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);

            setTimeOffset(SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS * 1000);
            pause(5000); //session length

            modAuthMellonProtectedResourcePage.navigateTo();
            URLAssert.assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);
        } finally {
            resetTimeOffset();
        }
    }

    @Test
    public void unauthorizedSSO() {
        modAuthMellonProtectedResourcePage2.navigateTo();
        URLAssert.assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);
        testRealmSAMLRedirectLoginPage.form().login("unauthorized", "password");
        assertTrue(driver.getPageSource().contains("Forbidden"));

        modAuthMellonProtectedResourcePage.navigateTo();
        assertTrue(driver.getPageSource().contains("Protected resource"));
        modAuthMellonProtectedResourcePage.logout();
    }

    @Test
    public void sessionExpiration() {
        try {
            modAuthMellonProtectedResourcePage.navigateTo();
            testRealmSAMLRedirectLoginPage.form().login(bburkeUser);
            assertTrue(driver.getPageSource().contains("Protected resource"));
            setTimeOffset(SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS * 1000);
            pause(5000); //session length

            modAuthMellonProtectedResourcePage.navigateTo();
            URLAssert.assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);
        } finally {
            resetTimeOffset();
        }
    }
}
