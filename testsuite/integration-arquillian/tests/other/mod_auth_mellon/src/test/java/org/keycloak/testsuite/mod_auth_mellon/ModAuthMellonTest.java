package org.keycloak.testsuite.mod_auth_mellon;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.util.URLAssert;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
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

        pause(5000); //session length

        modAuthMellonProtectedResourcePage.navigateTo();
        URLAssert.assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);
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
        modAuthMellonProtectedResourcePage.navigateTo();
        testRealmSAMLRedirectLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("Protected resource"));

        pause(5000); //session length

        modAuthMellonProtectedResourcePage.navigateTo();
        URLAssert.assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);
    }
}
