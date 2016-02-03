package org.keycloak.testsuite.mod_auth_mellon;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.util.URLAssert;

import javax.xml.transform.TransformerException;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;

/**
 * @author mhajas
 */
public class ModAuthMellonTest extends AbstractAuthTest {
    @Page
    private ModAuthMellonProtectedResource modAuthMellonProtectedResourcePage;

    @Page
    private ModAuthMellonUnprotectedResource modAuthMellonUnprotectedResourcePage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/mellon-realm.json"));
    }

    @Test
    public void modAuthMellonTest() throws TransformerException {
        testRealmPage.setAuthRealm("mellon-test");
        testRealmSAMLLoginPage.setAuthRealm("mellon-test");

        modAuthMellonUnprotectedResourcePage.navigateTo();
        assertTrue(driver.getPageSource().contains("Unprotected resource"));

        modAuthMellonProtectedResourcePage.navigateTo();
        URLAssert.assertCurrentUrlStartsWith(testRealmSAMLLoginPage);
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("Protected resource"));

        modAuthMellonProtectedResourcePage.logout();
        assertTrue(driver.getPageSource().contains("Unprotected resource"));

        modAuthMellonProtectedResourcePage.navigateTo();
        URLAssert.assertCurrentUrlStartsWith(testRealmSAMLLoginPage);
    }
}
