package org.keycloak.testsuite.adapter.servlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.adapters.rotation.PublicKeyLocator;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.Employee2Servlet;
import org.keycloak.testsuite.adapter.page.EmployeeSigServlet;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.auth.page.login.Login;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.openqa.selenium.By;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;

import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.getAppServerContextRoot;
import static org.keycloak.testsuite.auth.page.AuthRealm.SAMLSERVLETDEMO;
import static org.keycloak.testsuite.saml.AbstractSamlTest.SAML_CLIENT_ID_EMPLOYEE_2;
import static org.keycloak.testsuite.saml.AbstractSamlTest.SAML_CLIENT_ID_EMPLOYEE_SIG;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author mhajas
 */
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
// @AppServerContainer(ContainerConstants.APP_SERVER_EAP) // Should be added in: KEYCLOAK-14434
// @AppServerContainer(ContainerConstants.APP_SERVER_EAP6) // Should be added in: KEYCLOAK-14435
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT8)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT9)
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class SAMLSameSiteTest extends AbstractSAMLServletAdapterTest {
    private static final String NIP_IO_URL = "app-saml-127-0-0-1.nip.io";
    private static final String NIP_IO_EMPLOYEE2_URL = getAppServerContextRoot().replace("localhost", NIP_IO_URL) + "/employee2/";
    private static final String NIP_IO_EMPLOYEE_SIG_URL = getAppServerContextRoot().replace("localhost", NIP_IO_URL) + "/employee-sig/";

    @Deployment(name = Employee2Servlet.DEPLOYMENT_NAME)
    protected static WebArchive employee2() {
        return samlServletDeployment(Employee2Servlet.DEPLOYMENT_NAME, WEB_XML_WITH_ACTION_FILTER, SendUsernameServlet.class, AdapterActionsFilter.class, PublicKeyLocator.class)
                .addAsWebInfResource(undertowHandlersConf, UNDERTOW_HANDLERS_CONF);
    }

    @Deployment(name = EmployeeSigServlet.DEPLOYMENT_NAME)
    protected static WebArchive employeeSig() {
        return samlServletDeployment(EmployeeSigServlet.DEPLOYMENT_NAME, SendUsernameServlet.class)
                .addAsWebInfResource(undertowHandlersConf, UNDERTOW_HANDLERS_CONF);
    }

    @Page
    protected Employee2Servlet employee2ServletPage;

    @BeforeClass
    public static void enabledOnlyWithSSL() {
        ContainerAssume.assumeAuthServerSSL();
        ContainerAssume.assumeAppServerSSL();
    }

    @Test
    public void samlPostWorksWithSameSiteCookieTest() {
        testLoginLogoutWithDifferentUrl(SAML_CLIENT_ID_EMPLOYEE_2, NIP_IO_EMPLOYEE2_URL, testRealmSAMLPostLoginPage);
    }

    @Test
    public void samlRedirectWorksWithSameSiteCookieTest() {
        testLoginLogoutWithDifferentUrl(SAML_CLIENT_ID_EMPLOYEE_SIG, NIP_IO_EMPLOYEE_SIG_URL, testRealmSAMLRedirectLoginPage);
    }

    @Test
    public void testSSOPostRedirect() {
        getCleanup(SAMLSERVLETDEMO).addCleanup(ClientAttributeUpdater.forClient(adminClient, SAMLSERVLETDEMO, SAML_CLIENT_ID_EMPLOYEE_SIG)
                .setRedirectUris(Collections.singletonList(NIP_IO_EMPLOYEE_SIG_URL + "*"))
                .setAdminUrl(NIP_IO_EMPLOYEE_SIG_URL + "saml")
                .update());

        getCleanup(SAMLSERVLETDEMO).addCleanup(ClientAttributeUpdater.forClient(adminClient, SAMLSERVLETDEMO, SAML_CLIENT_ID_EMPLOYEE_2)
                .setRedirectUris(Collections.singletonList(NIP_IO_EMPLOYEE2_URL + "*"))
                .setAdminUrl(NIP_IO_EMPLOYEE2_URL + "saml")
                .update());

        // Navigate to url with nip.io to trick browser the adapter lives on different domain
        driver.navigate().to(NIP_IO_EMPLOYEE2_URL);
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);

        // Login and check the user is successfully logged in
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        waitUntilElement(By.xpath("//body")).text().contains("principal=bburke");

        driver.navigate().to(NIP_IO_EMPLOYEE_SIG_URL);
        waitUntilElement(By.xpath("//body")).text().contains("principal=bburke");

        // Logout
        driver.navigate().to(UriBuilder.fromUri(NIP_IO_EMPLOYEE_SIG_URL).queryParam("GLO", "true").build().toASCIIString());
        waitForPageToLoad();

        // Check logged out
        driver.navigate().to(NIP_IO_EMPLOYEE2_URL);
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
    }

    private void testLoginLogoutWithDifferentUrl(String clientId, String newUrl, Login loginPage) {
        getCleanup(SAMLSERVLETDEMO).addCleanup(ClientAttributeUpdater.forClient(adminClient, SAMLSERVLETDEMO, clientId)
                .setRedirectUris(Collections.singletonList(newUrl + "*"))
                .setAdminUrl(newUrl + "saml")
                .update());

        // Navigate to url with nip.io to trick browser the adapter lives on different domain
        driver.navigate().to(newUrl);
        assertCurrentUrlStartsWith(loginPage);

        // Login and check the user is successfully logged in
        loginPage.form().login(bburkeUser);
        waitUntilElement(By.xpath("//body")).text().contains("principal=bburke");

        // Logout
        driver.navigate().to(UriBuilder.fromUri(newUrl).queryParam("GLO", "true").build().toASCIIString());
        waitForPageToLoad();

        // Check logged out
        driver.navigate().to(newUrl);
        assertCurrentUrlStartsWith(loginPage);
    }


}
