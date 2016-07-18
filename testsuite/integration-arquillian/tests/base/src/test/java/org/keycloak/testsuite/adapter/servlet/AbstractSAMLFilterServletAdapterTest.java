package org.keycloak.testsuite.adapter.servlet;

import org.junit.After;
import org.junit.Before;
import org.keycloak.testsuite.arquillian.annotation.UseServletFilter;

/**
 * @author mhajas
 */

@UseServletFilter(filterName = "saml-filter", filterClass = "org.keycloak.adapters.saml.servlet.SamlFilter")
public abstract class AbstractSAMLFilterServletAdapterTest extends AbstractSAMLServletsAdapterTest {

    @Before
    public void checkRoles() {
        badClientSalesPostSigServletPage.checkRoles(true);
        badRealmSalesPostSigServletPage.checkRoles(true);
        employeeSigServletPage.checkRoles(true);
        employeeSigFrontServletPage.checkRoles(true);
        salesMetadataServletPage.checkRoles(true);
        salesPostServletPage.checkRoles(true);
        salesPostEncServletPage.checkRoles(true);
        salesPostSigServletPage.checkRoles(true);
        salesPostPassiveServletPage.checkRoles(true);
        salesPostSigEmailServletPage.checkRoles(true);
        salesPostSigPersistentServletPage.checkRoles(true);
        salesPostSigTransientServletPage.checkRoles(true);
        employee2ServletPage.navigateTo();

        //using endpoint instead of query param because we are not able to put query param to IDP initiated login
        testRealmLoginPage.form().login(bburkeUser);
        employee2ServletPage.checkRolesEndPoint();
        employee2ServletPage.logout();

        forbiddenIfNotAuthenticated = false;
    }

    @After
    public void uncheckRoles() {
        badClientSalesPostSigServletPage.checkRoles(false);
        badRealmSalesPostSigServletPage.checkRoles(false);
        employee2ServletPage.checkRoles(false);
        employeeSigServletPage.checkRoles(false);
        employeeSigFrontServletPage.checkRoles(false);
        salesMetadataServletPage.checkRoles(false);
        salesPostServletPage.checkRoles(false);
        salesPostEncServletPage.checkRoles(false);
        salesPostSigServletPage.checkRoles(false);
        salesPostPassiveServletPage.checkRoles(false);
        salesPostSigEmailServletPage.checkRoles(false);
        salesPostSigPersistentServletPage.checkRoles(false);
        salesPostSigTransientServletPage.checkRoles(false);
    }
}
