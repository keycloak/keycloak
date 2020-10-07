package org.keycloak.testsuite.adapter.servlet;

import org.jboss.arquillian.test.spi.execution.SkippedTestExecutionException;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.utils.annotation.UseServletFilter;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

/**
 * @author mhajas
 */
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_DEPRECATED)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@UseServletFilter(filterName = "saml-filter", filterClass = "org.keycloak.adapters.saml.servlet.SamlFilter",
        filterDependency = "org.keycloak:keycloak-saml-servlet-filter-adapter")
public class SAMLFilterServletAdapterTest extends SAMLServletAdapterTest {

    @BeforeClass
    public static void enabled() {
        String appServerJavaHome = System.getProperty("app.server.java.home", "");
        Assume.assumeFalse(appServerJavaHome.contains("1.7") || appServerJavaHome.contains("ibm-java-70"));

        // SAMLServletAdapterTest has too many deployments, with so many deployments (with filter dependency in each
        // of them) it is impossible to reload container after TLS is enabled, GC time limit exceeds
        ContainerAssume.assumeNotAppServerSSL();
    }

    @Before
    public void checkRoles() {
        badClientSalesPostSigServletPage.checkRoles(true);
        badRealmSalesPostSigServletPage.checkRoles(true);
        employeeAcsServletPage.checkRoles(true);
        employeeSigServletPage.checkRoles(true);
        employeeSigFrontServletPage.checkRoles(true);
        salesMetadataServletPage.checkRoles(true);
        salesPostServletPage.checkRoles(true);
        salesPostEncServletPage.checkRoles(true);
        salesPostEncSignAssertionsOnlyServletPage.checkRoles(true);
        salesPostSigServletPage.checkRoles(true);
        salesPostPassiveServletPage.checkRoles(true);
        salesPostSigPersistentServletPage.checkRoles(true);
        salesPostSigTransientServletPage.checkRoles(true);
        salesPostAssertionAndResponseSigPage.checkRoles(true);
        employeeSigPostNoIdpKeyServletPage.checkRoles(true);
        employeeSigRedirNoIdpKeyServletPage.checkRoles(true);
        employeeSigRedirOptNoIdpKeyServletPage.checkRoles(true);
        employeeRoleMappingPage.setupLoginInfo(testRealmSAMLPostLoginPage, bburkeUser);

        //using endpoint instead of query param because we are not able to put query param to IDP initiated login
        employee2ServletPage.navigateTo();
        testRealmLoginPage.form().login(bburkeUser);
        employee2ServletPage.checkRolesEndPoint(true);
        employee2ServletPage.logout();

        salesPostSigEmailServletPage.navigateTo();
        testRealmLoginPage.form().login(bburkeUser);
        salesPostSigEmailServletPage.checkRolesEndPoint(true);
        salesPostSigEmailServletPage.logout();
    }

    @After
    public void uncheckRoles() {
        badClientSalesPostSigServletPage.checkRoles(false);
        badRealmSalesPostSigServletPage.checkRoles(false);
        employeeAcsServletPage.checkRoles(false);
        employee2ServletPage.checkRoles(false);
        employeeSigServletPage.checkRoles(false);
        employeeSigFrontServletPage.checkRoles(false);
        salesMetadataServletPage.checkRoles(false);
        salesPostServletPage.checkRoles(false);
        salesPostEncServletPage.checkRoles(false);
        salesPostEncSignAssertionsOnlyServletPage.checkRoles(false);
        salesPostSigServletPage.checkRoles(false);
        salesPostPassiveServletPage.checkRoles(false);
        salesPostSigEmailServletPage.checkRoles(false);
        salesPostSigPersistentServletPage.checkRoles(false);
        salesPostSigTransientServletPage.checkRoles(false);
        employeeSigPostNoIdpKeyServletPage.checkRoles(false);
        employeeSigRedirNoIdpKeyServletPage.checkRoles(false);
        employeeSigRedirOptNoIdpKeyServletPage.checkRoles(false);
        employeeRoleMappingPage.clearLoginInfo();
    }

    @Test
    @Override
    @Ignore
    public void testSavedPostRequest() {

    }

    @Test
    @Override
    @Ignore
    public void multiTenant1SamlTest() throws Exception {

    }

    @Test
    @Override
    @Ignore
    public void multiTenant2SamlTest() throws Exception {

    }

    /**
     * Tests that the adapter is using the configured role mappings provider to map the roles extracted from the assertion
     * into roles that exist in the application domain. For this test a {@link org.keycloak.adapters.saml.PropertiesBasedRoleMapper}
     * has been setup in the adapter, performing the mappings as specified in the {@code role-mappings.properties} file.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    @Override
    public void testAdapterRoleMappings() throws Exception {
        try {
            employeeRoleMappingPage.setRolesToCheck("manager,coordinator,team-lead,employee");
            super.testAdapterRoleMappings();
        } finally {
            employeeRoleMappingPage.checkRolesEndPoint(false);
        }
    }
}
