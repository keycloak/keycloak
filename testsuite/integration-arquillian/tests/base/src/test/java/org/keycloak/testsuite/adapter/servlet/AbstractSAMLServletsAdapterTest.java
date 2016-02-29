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
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.*;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.login.SAMLIDPInitiatedLogin;
import org.keycloak.testsuite.util.IOUtil;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.auth.page.AuthRealm.SAMLSERVLETDEMO;
import static org.keycloak.testsuite.util.IOUtil.*;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 * @author mhajas
 */
public abstract class AbstractSAMLServletsAdapterTest extends AbstractServletsAdapterTest {
    @Page
    private BadClientSalesPostSigServlet badClientSalesPostSigServletPage;

    @Page
    private BadRealmSalesPostSigServlet badRealmSalesPostSigServletPage;

    @Page
    private Employee2Servlet employee2ServletPage;

    @Page
    private EmployeeSigServlet employeeSigServletPage;

    @Page
    private EmployeeSigFrontServlet employeeSigFrontServletPage;

    @Page
    private SalesMetadataServlet salesMetadataServletPage;

    @Page
    private SalesPostServlet salesPostServletPage;

    @Page
    private SalesPostEncServlet salesPostEncServletPage;

    @Page
    private SalesPostPassiveServlet salesPostPassiveServletPage;

    @Page
    private SalesPostSigServlet salesPostSigServletPage;

    @Page
    private SalesPostSigEmailServlet salesPostSigEmailServletPage;

    @Page
    private SalesPostSigPersistentServlet salesPostSigPersistentServletPage;

    @Page
    private SalesPostSigTransientServlet salesPostSigTransientServletPage;

    @Page
    private SAMLIDPInitiatedLogin samlidpInitiatedLogin;

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

    @Test
    public void disabledClientTest() {
        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), "http://localhost:8081/sales-post-sig/");
        ClientRepresentation client = clientResource.toRepresentation();
        client.setEnabled(false);
        clientResource.update(client);

        salesPostSigServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("Login requester not enabled"));

        client.setEnabled(true);
        clientResource.update(client);
    }

    @Test
    public void unauthorizedSSOTest() {
        salesPostServletPage.navigateTo();
        testRealmSAMLRedirectLoginPage.form().login("unauthorized", "password");

        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));

        employee2ServletPage.navigateTo();
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));

        employeeSigFrontServletPage.navigateTo();
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));

        salesPostSigPersistentServletPage.navigateTo();
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));

        salesPostServletPage.logout();
    }

    @Test
    public void singleLoginAndLogoutSAMLTest() {
        salesPostServletPage.navigateTo();
        testRealmSAMLRedirectLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostSigServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        employee2ServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostEncServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        employeeSigFrontServletPage.logout();

        employeeSigFrontServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);

        employeeSigServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);

        salesPostPassiveServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("<body></body>") || driver.getPageSource().contains("<body><pre></pre></body>"));

        salesPostSigEmailServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
    }

    @Test
    public void badClientSalesPostSigTest() {
        badClientSalesPostSigServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("invalidRequesterMessage"));
    }

    @Test
    public void badRealmSalesPostSigTest() {
        badRealmSalesPostSigServletPage.navigateTo();
        testRealmSAMLRedirectLoginPage.form().login(bburkeUser);

        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
    }

    @Test
    public void employee2Test() {
        employee2ServletPage.navigateTo();
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        employee2ServletPage.logout();
        employee2ServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);

        testRealmSAMLPostLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
        employee2ServletPage.logout();
    }

    @Test
    public void employeeSigTest() {
        employeeSigServletPage.navigateTo();
        testRealmSAMLRedirectLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        employeeSigServletPage.logout();
        employeeSigServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);

        testRealmSAMLRedirectLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
        employeeSigServletPage.logout();
    }

    @Test
    public void employeeSigFrontTest() {
        employeeSigFrontServletPage.navigateTo();
        testRealmSAMLRedirectLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        employeeSigFrontServletPage.logout();
        employeeSigFrontServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLRedirectLoginPage);

        testRealmSAMLRedirectLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
        employeeSigFrontServletPage.logout();
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

        salesMetadataServletPage.navigateTo();
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesMetadataServletPage.logout();
        salesMetadataServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);

        testRealmSAMLPostLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
        salesMetadataServletPage.logout();
    }

    @Test
    public void salesPostTest() {
        salesPostServletPage.navigateTo();
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostServletPage.logout();
        salesPostServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);

        testRealmSAMLPostLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
        salesPostServletPage.logout();
    }

    @Test
    public void salesPostEncTest() {
        salesPostEncServletPage.navigateTo();
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostEncServletPage.logout();
        salesPostEncServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);

        testRealmSAMLPostLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
        salesPostEncServletPage.logout();
    }

    @Test
    public void salesPostPassiveTest() {
        salesPostPassiveServletPage.navigateTo();
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("<body></body>") || driver.getPageSource().contains("<body><pre></pre></body>"));

        salesPostServletPage.navigateTo();
        testRealmSAMLRedirectLoginPage.form().login(bburkeUser);

        salesPostPassiveServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostPassiveServletPage.logout();
        salesPostPassiveServletPage.navigateTo();
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("<body></body>") || driver.getPageSource().contains("<body><pre></pre></body>"));

        salesPostServletPage.navigateTo();
        testRealmSAMLRedirectLoginPage.form().login("unauthorized", "password");
        salesPostPassiveServletPage.navigateTo();
        assertFalse(driver.getPageSource().contains("principal="));
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
        salesPostPassiveServletPage.logout();
    }

    @Test
    public void salesPostSigTest() {
        salesPostEncServletPage.navigateTo();
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostEncServletPage.logout();
        salesPostEncServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);

        testRealmSAMLPostLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
        salesPostEncServletPage.logout();
    }

    @Test
    public void salesPostSigEmailTest() {
        salesPostSigEmailServletPage.navigateTo();
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostSigEmailServletPage.logout();
        salesPostSigEmailServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);

        testRealmSAMLPostLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
        salesPostSigEmailServletPage.logout();
    }

    @Test
    public void salesPostSigPersistentTest() {
        salesPostSigPersistentServletPage.navigateTo();
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        assertFalse(driver.getPageSource().contains("bburke"));
        assertTrue(driver.getPageSource().contains("principal=G-"));

        salesPostSigPersistentServletPage.logout();
        salesPostSigPersistentServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);

        testRealmSAMLPostLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
        salesPostSigPersistentServletPage.logout();
    }

    @Test
    public void salesPostSigTransientTest() {
        salesPostSigTransientServletPage.navigateTo();
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        assertFalse(driver.getPageSource().contains("bburke"));
        assertTrue(driver.getPageSource().contains("principal=G-"));

        salesPostSigTransientServletPage.logout();
        salesPostSigTransientServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);

        testRealmSAMLPostLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));
        salesPostSigTransientServletPage.logout();
    }

    @Test
    public void  idpInitiatedLogin() {
        samlidpInitiatedLogin.setAuthRealm(SAMLSERVLETDEMO);
        samlidpInitiatedLogin.setUrlName("employee2");
        samlidpInitiatedLogin.navigateTo();
        samlidpInitiatedLogin.form().login(bburkeUser);

        employee2ServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostSigServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        employee2ServletPage.logout();
    }

    @Test
    public void idpInitiatedUnauthorizedLoginTest() {
        samlidpInitiatedLogin.setAuthRealm(SAMLSERVLETDEMO);
        samlidpInitiatedLogin.setUrlName("employee2");
        samlidpInitiatedLogin.navigateTo();
        samlidpInitiatedLogin.form().login("unauthorized","password");

        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));

        employee2ServletPage.navigateTo();
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains("Status 403"));

        employee2ServletPage.logout();
    }
}
