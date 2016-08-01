/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.adapter.servlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.*;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.login.Login;
import org.keycloak.testsuite.auth.page.login.SAMLIDPInitiatedLogin;
import org.keycloak.testsuite.page.AbstractPage;
import org.keycloak.testsuite.util.IOUtil;
import org.openqa.selenium.By;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.SAMLSERVLETDEMO;
import static org.keycloak.testsuite.util.IOUtil.*;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author mhajas
 */
public abstract class AbstractSAMLServletsAdapterTest extends AbstractServletsAdapterTest {
    @Page
    protected BadClientSalesPostSigServlet badClientSalesPostSigServletPage;

    @Page
    protected BadRealmSalesPostSigServlet badRealmSalesPostSigServletPage;

    @Page
    protected Employee2Servlet employee2ServletPage;

    @Page
    protected EmployeeSigServlet employeeSigServletPage;

    @Page
    protected EmployeeSigFrontServlet employeeSigFrontServletPage;

    @Page
    protected SalesMetadataServlet salesMetadataServletPage;

    @Page
    protected SalesPostServlet salesPostServletPage;

    @Page
    protected SalesPostEncServlet salesPostEncServletPage;

    @Page
    protected SalesPostPassiveServlet salesPostPassiveServletPage;

    @Page
    protected SalesPostSigServlet salesPostSigServletPage;

    @Page
    protected SalesPostSigEmailServlet salesPostSigEmailServletPage;

    @Page
    protected SalesPostSigPersistentServlet salesPostSigPersistentServletPage;

    @Page
    protected SalesPostSigTransientServlet salesPostSigTransientServletPage;

    @Page
    protected SAMLIDPInitiatedLogin samlidpInitiatedLogin;

    protected boolean forbiddenIfNotAuthenticated = true;

    @Deployment(name = BadClientSalesPostSigServlet.DEPLOYMENT_NAME)
    protected static WebArchive badClientSalesPostSig() {
        return samlServletDeployment(BadClientSalesPostSigServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = BadRealmSalesPostSigServlet.DEPLOYMENT_NAME)
    protected static WebArchive badRealmSalesPostSig() {
        return samlServletDeployment(BadRealmSalesPostSigServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = Employee2Servlet.DEPLOYMENT_NAME)
    protected static WebArchive employee2() {
        return samlServletDeployment(Employee2Servlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = EmployeeSigServlet.DEPLOYMENT_NAME)
    protected static WebArchive employeeSig() {
        return samlServletDeployment(EmployeeSigServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = EmployeeSigFrontServlet.DEPLOYMENT_NAME)
    protected static WebArchive employeeSigFront() {
        return samlServletDeployment(EmployeeSigFrontServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = SalesMetadataServlet.DEPLOYMENT_NAME)
    protected static WebArchive salesMetadata() {
        return samlServletDeployment(SalesMetadataServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = SalesPostServlet.DEPLOYMENT_NAME)
    protected static WebArchive salesPost() {
        return samlServletDeployment(SalesPostServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = SalesPostEncServlet.DEPLOYMENT_NAME)
    protected static WebArchive salesPostEnc() {
        return samlServletDeployment(SalesPostEncServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = SalesPostPassiveServlet.DEPLOYMENT_NAME)
    protected static WebArchive salesPostPassive() {
        return samlServletDeployment(SalesPostPassiveServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = SalesPostSigServlet.DEPLOYMENT_NAME)
    protected static WebArchive salesPostSig() {
        return samlServletDeployment(SalesPostSigServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = SalesPostSigEmailServlet.DEPLOYMENT_NAME)
    protected static WebArchive salesPostSigEmail() {
        return samlServletDeployment(SalesPostSigEmailServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = SalesPostSigPersistentServlet.DEPLOYMENT_NAME)
    protected static WebArchive salesPostSigPersistent() {
        return samlServletDeployment(SalesPostSigPersistentServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = SalesPostSigTransientServlet.DEPLOYMENT_NAME)
    protected static WebArchive salesPostSigTransient() {
        return samlServletDeployment(SalesPostSigTransientServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/keycloak-saml/testsaml.json"));
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(SAMLSERVLETDEMO);
        testRealmSAMLRedirectLoginPage.setAuthRealm(SAMLSERVLETDEMO);
        testRealmSAMLPostLoginPage.setAuthRealm(SAMLSERVLETDEMO);
    }

    private void assertForbidden(AbstractPage page) {
        page.navigateTo();
        waitUntilElement(By.xpath("//body")).text().not().contains("principal=");
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
    }

    private void assertSuccessfullyLoggedIn(AbstractPage page) {
        page.navigateTo();
        waitUntilElement(By.xpath("//body")).text().contains("principal=bburke");
    }

    private void assertForbiddenLogin(AbstractPage page, String username, String password, Login loginPage) {
        page.navigateTo();
        assertCurrentUrlStartsWith(loginPage);
        loginPage.form().login(username, password);
        waitUntilElement(By.xpath("//body")).text().not().contains("principal=");
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
    }

    private void assertSuccessfulLogin(AbstractPage page, UserRepresentation user, Login loginPage) {
        page.navigateTo();
        assertCurrentUrlStartsWith(loginPage);
        loginPage.form().login(user);
        waitUntilElement(By.xpath("//body")).text().contains("principal=bburke");
    }

    private void testSuccessfulAndUnauthorizedLogin(SAMLServlet page, Login loginPage) {
        assertSuccessfulLogin(page, bburkeUser, loginPage);
        page.logout();
        assertForbiddenLogin(page, "unauthorized", "password", loginPage);
        page.logout();
    }

    @Test
    public void disabledClientTest() {
        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), "http://localhost:8081/sales-post-sig/");
        ClientRepresentation client = clientResource.toRepresentation();
        client.setEnabled(false);
        clientResource.update(client);

        salesPostSigServletPage.navigateTo();
        waitUntilElement(By.xpath("//body")).text().contains("Login requester not enabled");

        client.setEnabled(true);
        clientResource.update(client);
    }

    @Test
    public void unauthorizedSSOTest() {
        assertForbiddenLogin(salesPostServletPage, "unauthorized", "password", testRealmSAMLPostLoginPage);
        assertForbidden(employee2ServletPage);
        assertForbidden(employeeSigFrontServletPage);
        assertForbidden(salesPostSigPersistentServletPage);
        salesPostServletPage.logout();
    }

    @Test
    public void singleLoginAndLogoutSAMLTest() {
        assertSuccessfulLogin(salesPostServletPage, bburkeUser, testRealmSAMLPostLoginPage);
        assertSuccessfullyLoggedIn(salesPostSigServletPage);
        assertSuccessfullyLoggedIn(employee2ServletPage);
        assertSuccessfullyLoggedIn(salesPostEncServletPage);

        employeeSigFrontServletPage.logout();

        employeeSigFrontServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);

        employeeSigServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);

        salesPostPassiveServletPage.navigateTo();
        if (forbiddenIfNotAuthenticated) {
            waitUntilElement(By.xpath("//body")).text().not().contains("principal=");
            assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("<body></body>") || driver.getPageSource().equals(""));
        } else {
            waitUntilElement(By.xpath("//body")).text().contains("principal=null");
        }

        salesPostSigEmailServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
    }

    @Test
    public void badClientSalesPostSigTest() {
        badClientSalesPostSigServletPage.navigateTo();
        waitUntilElement(By.xpath("//body")).text().contains("Invalid requester");
    }

    @Test
    public void badRealmSalesPostSigTest() {
        badRealmSalesPostSigServletPage.navigateTo();
        testRealmSAMLRedirectLoginPage.form().login(bburkeUser);

        waitUntilElement(By.xpath("//body")).text().not().contains("principal=");
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
    }

    @Test
    public void employee2Test() {
        testSuccessfulAndUnauthorizedLogin(employee2ServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void employeeSigTest() {
        testSuccessfulAndUnauthorizedLogin(employeeSigServletPage, testRealmSAMLRedirectLoginPage);
    }

    @Test
    public void employeeSigFrontTest() {
        testSuccessfulAndUnauthorizedLogin(employeeSigFrontServletPage, testRealmSAMLRedirectLoginPage);
    }

    @Test
    public void salesMetadataTest() throws Exception {
        Document doc = loadXML(AbstractSAMLServletsAdapterTest.class.getResourceAsStream("/adapter-test/keycloak-saml/sp-metadata.xml"));

        modifyDocElementAttribute(doc, "SingleLogoutService", "Location", "8080", System.getProperty("app.server.http.port", null));
        modifyDocElementAttribute(doc, "AssertionConsumerService", "Location", "8080", System.getProperty("app.server.http.port", null));

        ClientRepresentation clientRep = testRealmResource().convertClientDescription(IOUtil.documentToString(doc));

        String appServerUrl;
        if (Boolean.parseBoolean(System.getProperty("app.server.ssl.required"))) {
            appServerUrl = "https://localhost:" + System.getProperty("app.server.https.port", "8543") + "/";
        } else {
            appServerUrl = "http://localhost:" + System.getProperty("app.server.http.port", "8280") + "/";
        }

        clientRep.setAdminUrl(appServerUrl + "sales-metadata/saml");

        Response response = testRealmResource().clients().create(clientRep);
        assertEquals(201, response.getStatus());
        response.close();

        testSuccessfulAndUnauthorizedLogin(salesMetadataServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void salesPostTest() {
        testSuccessfulAndUnauthorizedLogin(salesPostServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void salesPostEncTest() {
        testSuccessfulAndUnauthorizedLogin(salesPostEncServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void salesPostPassiveTest() {
        salesPostPassiveServletPage.navigateTo();

        if (forbiddenIfNotAuthenticated) {
            waitUntilElement(By.xpath("//body")).text().not().contains("principal=");
            //Different 403 status page on EAP and Wildfly
            assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("<body></body>") || driver.getPageSource().equals(""));
        } else {
            waitUntilElement(By.xpath("//body")).text().contains("principal=null");
        }

        assertSuccessfulLogin(salesPostServletPage, bburkeUser, testRealmSAMLPostLoginPage);

        assertSuccessfullyLoggedIn(salesPostPassiveServletPage);

        salesPostPassiveServletPage.logout();
        salesPostPassiveServletPage.navigateTo();

        if (forbiddenIfNotAuthenticated) {
            waitUntilElement(By.xpath("//body")).text().not().contains("principal=");
            //Different 403 status page on EAP and Wildfly
            assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("<body></body>") || driver.getPageSource().equals(""));
        } else {
            waitUntilElement(By.xpath("//body")).text().contains("principal=null");
        }
        assertForbiddenLogin(salesPostServletPage, "unauthorized", "password", testRealmSAMLPostLoginPage);
        assertForbidden(salesPostPassiveServletPage);

        salesPostPassiveServletPage.logout();
    }

    @Test
    public void salesPostSigTest() {
        testSuccessfulAndUnauthorizedLogin(salesPostSigServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void salesPostSigEmailTest() {
        testSuccessfulAndUnauthorizedLogin(salesPostSigEmailServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void salesPostSigPersistentTest() {
        salesPostSigPersistentServletPage.navigateTo();
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        waitUntilElement(By.xpath("//body")).text().not().contains("bburke");
        waitUntilElement(By.xpath("//body")).text().contains("principal=G-");

        salesPostSigPersistentServletPage.logout();

        assertForbiddenLogin(salesPostSigPersistentServletPage, "unauthorized", "password", testRealmSAMLPostLoginPage);
        salesPostSigPersistentServletPage.logout();
    }

    @Test
    public void salesPostSigTransientTest() {
        salesPostSigTransientServletPage.navigateTo();
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        waitUntilElement(By.xpath("//body")).text().not().contains("bburke");
        waitUntilElement(By.xpath("//body")).text().contains("principal=G-");

        salesPostSigTransientServletPage.logout();

        assertForbiddenLogin(salesPostSigTransientServletPage, "unauthorized", "password", testRealmSAMLPostLoginPage);
        salesPostSigTransientServletPage.logout();
    }

    @Test
    public void idpInitiatedLogin() {
        samlidpInitiatedLogin.setAuthRealm(SAMLSERVLETDEMO);
        samlidpInitiatedLogin.setUrlName("employee2");
        samlidpInitiatedLogin.navigateTo();
        samlidpInitiatedLogin.form().login(bburkeUser);

        waitUntilElement(By.xpath("//body")).text().contains("principal=bburke");

        assertSuccessfullyLoggedIn(salesPostSigServletPage);

        employee2ServletPage.logout();
    }

    @Test
    public void idpInitiatedUnauthorizedLoginTest() {
        samlidpInitiatedLogin.setAuthRealm(SAMLSERVLETDEMO);
        samlidpInitiatedLogin.setUrlName("employee2");
        samlidpInitiatedLogin.navigateTo();
        samlidpInitiatedLogin.form().login("unauthorized", "password");

        waitUntilElement(By.xpath("//body")).text().not().contains("bburke");
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));

        assertForbidden(employee2ServletPage);
        employee2ServletPage.logout();
    }
}
