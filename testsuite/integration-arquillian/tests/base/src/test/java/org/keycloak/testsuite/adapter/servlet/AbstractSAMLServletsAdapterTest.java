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
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.RoleListMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2ErrorResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.*;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.login.Login;
import org.keycloak.testsuite.auth.page.login.SAMLIDPInitiatedLogin;
import org.keycloak.testsuite.page.AbstractPage;
import org.keycloak.testsuite.util.IOUtil;
import org.openqa.selenium.By;
import org.w3c.dom.Document;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private SalesPost2Servlet salesPost2ServletPage;

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

    @Page
    protected SalesPostAssertionAndResponseSig salesPostAssertionAndResponseSigPage;

    @Page
    protected BadAssertionSalesPostSig badAssertionSalesPostSigPage;

    @Page
    protected MissingAssertionSig missingAssertionSigPage;

    @Page
    protected EmployeeServlet employeeServletPage;

    @Page
    private InputPortal inputPortalPage;

    @Page
    private SAMLIDPInitiatedLogin samlidpInitiatedLoginPage;

    public static final String FORBIDDEN_TEXT = "HTTP status code: 403";

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

    @Deployment(name = InputPortal.DEPLOYMENT_NAME)
    protected static WebArchive inputPortal() {
        return samlServletDeployment(InputPortal.DEPLOYMENT_NAME, "input-portal/WEB-INF/web.xml" , InputServlet.class);
    }

    @Deployment(name = SalesPost2Servlet.DEPLOYMENT_NAME)
    protected static WebArchive salesPost2() {
        return samlServletDeployment(SalesPost2Servlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = SalesPostAssertionAndResponseSig.DEPLOYMENT_NAME)
    protected static WebArchive salesPostAssertionAndResponseSig() {
        return samlServletDeployment(SalesPostAssertionAndResponseSig.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = BadAssertionSalesPostSig.DEPLOYMENT_NAME)
    protected static WebArchive badAssertionSalesPostSig() {
        return samlServletDeployment(BadAssertionSalesPostSig.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = MissingAssertionSig.DEPLOYMENT_NAME)
    protected static WebArchive missingAssertionSig() {
        return samlServletDeployment(MissingAssertionSig.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = EmployeeServlet.DEPLOYMENT_NAME)
    protected static WebArchive employeeServlet() {
        return samlServletDeployment(EmployeeServlet.DEPLOYMENT_NAME, "employee/WEB-INF/web.xml", SamlSPFacade.class);
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

    private void assertForbidden(AbstractPage page, String expectedNotContains) {
        page.navigateTo();
        waitUntilElement(By.xpath("//body")).text().not().contains(expectedNotContains);
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains(FORBIDDEN_TEXT));
    }

    private void assertSuccessfullyLoggedIn(AbstractPage page, String expectedText) {
        page.navigateTo();
        waitUntilElement(By.xpath("//body")).text().contains(expectedText);
    }

    private void assertForbiddenLogin(AbstractPage page, String username, String password, Login loginPage, String expectedNotContains) {
        page.navigateTo();
        assertCurrentUrlStartsWith(loginPage);
        loginPage.form().login(username, password);
        waitUntilElement(By.xpath("//body")).text().not().contains(expectedNotContains);
        //Different 403 status page on EAP and Wildfly
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains(FORBIDDEN_TEXT));
    }

    private void assertSuccessfulLogin(AbstractPage page, UserRepresentation user, Login loginPage, String expectedString) {
        page.navigateTo();
        assertCurrentUrlStartsWith(loginPage);
        loginPage.form().login(user);
        waitUntilElement(By.xpath("//body")).text().contains(expectedString);
    }

    private void testSuccessfulAndUnauthorizedLogin(SAMLServlet page, Login loginPage) {
        testSuccessfulAndUnauthorizedLogin(page, loginPage, "principal=bburke");
    }

    private void testSuccessfulAndUnauthorizedLogin(SAMLServlet page, Login loginPage, String expectedText) {
        testSuccessfulAndUnauthorizedLogin(page, loginPage, expectedText, "principal=");
    }

    private void testSuccessfulAndUnauthorizedLogin(SAMLServlet page, Login loginPage, String expectedText, String expectedNotContains) {
        assertSuccessfulLogin(page, bburkeUser, loginPage, expectedText);
        page.logout();
        checkLoggedOut(page, loginPage);
        assertForbiddenLogin(page, "unauthorized", "password", loginPage, expectedNotContains);
        page.logout();
        checkLoggedOut(page, loginPage);
    }

    private void checkLoggedOut(AbstractPage page, Login loginPage) {
        page.navigateTo();
        waitUntilElement(By.xpath("//body")).is().present();
        assertCurrentUrlStartsWith(loginPage);
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
        assertForbiddenLogin(salesPostServletPage, "unauthorized", "password", testRealmSAMLPostLoginPage, "principal=");
        assertForbidden(employee2ServletPage, "principal=");
        assertForbidden(employeeSigFrontServletPage, "principal=");
        assertForbidden(salesPostSigPersistentServletPage, "principal=");
        salesPostServletPage.logout();
        checkLoggedOut(salesPostServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void singleLoginAndLogoutSAMLTest() {
        assertSuccessfulLogin(salesPostServletPage, bburkeUser, testRealmSAMLPostLoginPage, "principal=bburke");
        assertSuccessfullyLoggedIn(salesPostSigServletPage, "principal=bburke");
        assertSuccessfullyLoggedIn(employee2ServletPage, "principal=bburke");
        assertSuccessfullyLoggedIn(salesPostEncServletPage, "principal=bburke");

        employeeSigFrontServletPage.logout();

        checkLoggedOut(employeeSigFrontServletPage, testRealmSAMLRedirectLoginPage);
        checkLoggedOut(employeeSigServletPage, testRealmSAMLRedirectLoginPage);

        salesPostPassiveServletPage.navigateTo();
        if (forbiddenIfNotAuthenticated) {
            assertOnForbiddenPage();
        } else {
            waitUntilElement(By.xpath("//body")).text().contains("principal=null");
        }

        checkLoggedOut(salesPostSigEmailServletPage, testRealmSAMLPostLoginPage);
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
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains(FORBIDDEN_TEXT));
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
            assertOnForbiddenPage();
        } else {
            waitUntilElement(By.xpath("//body")).text().contains("principal=null");
        }

        assertSuccessfulLogin(salesPostServletPage, bburkeUser, testRealmSAMLPostLoginPage, "principal=bburke");

        assertSuccessfullyLoggedIn(salesPostPassiveServletPage, "principal=bburke");

        salesPostPassiveServletPage.logout();
        salesPostPassiveServletPage.navigateTo();

        if (forbiddenIfNotAuthenticated) {
            assertOnForbiddenPage();
        } else {
            waitUntilElement(By.xpath("//body")).text().contains("principal=null");
        }

        assertForbiddenLogin(salesPostServletPage, "unauthorized", "password", testRealmSAMLPostLoginPage, "principal=");
        assertForbidden(salesPostPassiveServletPage, "principal=");

        salesPostPassiveServletPage.logout();
    }

    @Test
    public void salesPostSigTest() {
        testSuccessfulAndUnauthorizedLogin(salesPostSigServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void salesPostSigEmailTest() {
        testSuccessfulAndUnauthorizedLogin(salesPostSigEmailServletPage, testRealmSAMLPostLoginPage, "principal=bburke@redhat.com");
    }

    @Test
    public void salesPostSigPersistentTest() {
        salesPostSigPersistentServletPage.navigateTo();
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        waitUntilElement(By.xpath("//body")).text().not().contains("bburke");
        waitUntilElement(By.xpath("//body")).text().contains("principal=G-");

        salesPostSigPersistentServletPage.logout();
        checkLoggedOut(salesPostSigPersistentServletPage, testRealmSAMLPostLoginPage);

        assertForbiddenLogin(salesPostSigPersistentServletPage, "unauthorized", "password", testRealmSAMLPostLoginPage, "principal=");
        salesPostSigPersistentServletPage.logout();
        checkLoggedOut(salesPostSigPersistentServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void salesPostSigTransientTest() {
        salesPostSigTransientServletPage.navigateTo();
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        waitUntilElement(By.xpath("//body")).text().not().contains("bburke");
        waitUntilElement(By.xpath("//body")).text().contains("principal=G-");

        salesPostSigTransientServletPage.logout();
        checkLoggedOut(salesPostSigTransientServletPage, testRealmSAMLPostLoginPage);

        assertForbiddenLogin(salesPostSigTransientServletPage, "unauthorized", "password", testRealmSAMLPostLoginPage, "principal=");
        salesPostSigTransientServletPage.logout();
        checkLoggedOut(salesPostSigTransientServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void idpInitiatedLogin() {
        samlidpInitiatedLoginPage.setAuthRealm(SAMLSERVLETDEMO);
        samlidpInitiatedLoginPage.setUrlName("employee2");
        samlidpInitiatedLoginPage.navigateTo();
        samlidpInitiatedLoginPage.form().login(bburkeUser);

        waitUntilElement(By.xpath("//body")).text().contains("principal=bburke");

        assertSuccessfullyLoggedIn(salesPostSigServletPage, "principal=bburke");

        employee2ServletPage.logout();
        checkLoggedOut(employee2ServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void idpInitiatedUnauthorizedLoginTest() {
        samlidpInitiatedLoginPage.setAuthRealm(SAMLSERVLETDEMO);
        samlidpInitiatedLoginPage.setUrlName("employee2");
        samlidpInitiatedLoginPage.navigateTo();
        samlidpInitiatedLoginPage.form().login("unauthorized", "password");

        waitUntilElement(By.xpath("//body")).text().not().contains("bburke");
        assertTrue(driver.getPageSource().contains("Forbidden") || driver.getPageSource().contains(FORBIDDEN_TEXT));

        assertForbidden(employee2ServletPage, "principal=");
        employee2ServletPage.logout();
        checkLoggedOut(employee2ServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void testSavedPostRequest() {
        inputPortalPage.navigateTo();
        assertCurrentUrlStartsWith(inputPortalPage);
        inputPortalPage.execute("hello");

        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        Assert.assertEquals(driver.getCurrentUrl(), inputPortalPage + "/secured/post");
        waitUntilElement(By.xpath("//body")).text().contains("parameter=hello");

        // test that user principal and KeycloakSecurityContext available
        driver.navigate().to(inputPortalPage + "/insecure");
        waitUntilElement(By.xpath("//body")).text().contains("Insecure Page");

        if (System.getProperty("insecure.user.principal.unsupported") == null) waitUntilElement(By.xpath("//body")).text().contains("UserPrincipal");

        // test logout

        inputPortalPage.logout();

        // test unsecured POST KEYCLOAK-901

        Client client = ClientBuilder.newClient();
        Form form = new Form();
        form.param("parameter", "hello");
        String text = client.target(inputPortalPage + "/unsecured").request().post(Entity.form(form), String.class);
        Assert.assertTrue(text.contains("parameter=hello"));
        client.close();
    }

    @Test
    public void testPostSimpleLoginLogoutIdpInitiatedRedirectTo() {
        samlidpInitiatedLoginPage.setAuthRealm(SAMLSERVLETDEMO);
        samlidpInitiatedLoginPage.setUrlName("sales-post2");
        samlidpInitiatedLoginPage.navigateTo();

        samlidpInitiatedLoginPage.form().login(bburkeUser);
        assertCurrentUrlStartsWith(salesPost2ServletPage);
        assertTrue(driver.getCurrentUrl().endsWith("/foo"));
        waitUntilElement(By.xpath("//body")).text().contains("principal=bburke");
        salesPost2ServletPage.logout();
        checkLoggedOut(salesPost2ServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void salesPostAssertionAndResponseSigTest() {
        testSuccessfulAndUnauthorizedLogin(salesPostAssertionAndResponseSigPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void testPostBadAssertionSignature() {
        badAssertionSalesPostSigPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login("bburke", "password");

        waitUntilElement(By.xpath("//body")).text().contains("Error info: SamlAuthenticationError [reason=INVALID_SIGNATURE, status=null]");
        assertEquals(driver.getCurrentUrl(), badAssertionSalesPostSigPage + "/saml");
    }

    @Test
    public void testMissingAssertionSignature() {
        missingAssertionSigPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login("bburke", "password");

        waitUntilElement(By.xpath("//body")).text().contains("Error info: SamlAuthenticationError [reason=INVALID_SIGNATURE, status=null]");
        assertEquals(driver.getCurrentUrl(), missingAssertionSigPage + "/saml");
    }

    @Test
    public void testErrorHandling() throws Exception {
        Client client = ClientBuilder.newClient();
        // make sure
        Response response = client.target(employeeSigServletPage.toString()).request().get();
        response.close();
        SAML2ErrorResponseBuilder builder = new SAML2ErrorResponseBuilder()
                .destination(employeeSigServletPage.toString() + "/saml")
                .issuer("http://localhost:" + System.getProperty("auth.server.http.port", "8180") + "/realms/demo")
                .status(JBossSAMLURIConstants.STATUS_REQUEST_DENIED.get());
        BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder()
                .relayState(null);
        Document document = builder.buildDocument();
        URI uri = binding.redirectBinding(document).generateURI(employeeSigServletPage.toString() + "/saml", false);
        response = client.target(uri).request().get();
        String errorPage = response.readEntity(String.class);
        response.close();
        Assert.assertTrue(errorPage.contains("Error info: SamlAuthenticationError [reason=ERROR_STATUS"));
        Assert.assertFalse(errorPage.contains("status=null"));
        client.close();
    }

    @Test
    public void testRelayStateEncoding() throws Exception {
        // this test has a hardcoded SAMLRequest and we hack a SP face servlet to get the SAMLResponse so we can look
        // at the relay state
        employeeServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login("bburke", "password");
        assertCurrentUrlStartsWith(employeeServletPage);
        waitUntilElement(By.xpath("//body")).text().contains("Relay state: " + SamlSPFacade.RELAY_STATE);
        waitUntilElement(By.xpath("//body")).text().not().contains("SAML response: null");
    }

    @Test
    public void testAttributes() throws Exception {
        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), "http://localhost:8081/employee2/");
        ProtocolMappersResource protocolMappersResource = clientResource.getProtocolMappers();

        Map<String, String> config = new LinkedHashMap<>();
        config.put("attribute.nameformat", "Basic");
        config.put("user.attribute", "topAttribute");
        config.put("attribute.name", "topAttribute");
        createProtocolMapper(protocolMappersResource, "topAttribute", "saml", "saml-user-attribute-mapper", config);

        config = new LinkedHashMap<>();
        config.put("attribute.nameformat", "Basic");
        config.put("user.attribute", "level2Attribute");
        config.put("attribute.name", "level2Attribute");
        createProtocolMapper(protocolMappersResource, "level2Attribute", "saml", "saml-user-attribute-mapper", config);

        config = new LinkedHashMap<>();
        config.put("attribute.nameformat", "Basic");
        config.put("single", "true");
        config.put("attribute.name", "group");
        createProtocolMapper(protocolMappersResource, "groups", "saml", "saml-group-membership-mapper", config);

        setRolesToCheck("manager,user");

        employee2ServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login("level2GroupUser", "password");

        driver.navigate().to(employee2ServletPage.toString() + "/getAttributes");
        waitUntilElement(By.xpath("//body")).text().contains("topAttribute: true");
        waitUntilElement(By.xpath("//body")).text().contains("level2Attribute: true");
        waitUntilElement(By.xpath("//body")).text().contains("attribute email: level2@redhat.com");
        waitUntilElement(By.xpath("//body")).text().not().contains("group: []");
        waitUntilElement(By.xpath("//body")).text().not().contains("group: null");
        waitUntilElement(By.xpath("//body")).text().contains("group: [level2]");

        employee2ServletPage.logout();
        checkLoggedOut(employee2ServletPage, testRealmSAMLPostLoginPage);

        setRolesToCheck("manager,employee,user");

        employee2ServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login(bburkeUser);

        driver.navigate().to(employee2ServletPage.toString() + "/getAttributes");
        waitUntilElement(By.xpath("//body")).text().contains("attribute email: bburke@redhat.com");
        waitUntilElement(By.xpath("//body")).text().contains("friendlyAttribute email: bburke@redhat.com");
        waitUntilElement(By.xpath("//body")).text().contains("phone: 617");
        waitUntilElement(By.xpath("//body")).text().contains("friendlyAttribute phone: null");

        employee2ServletPage.logout();
        checkLoggedOut(employee2ServletPage, testRealmSAMLPostLoginPage);

        config = new LinkedHashMap<>();
        config.put("attribute.value", "hard");
        config.put("attribute.nameformat", "Basic");
        config.put("attribute.name", "hardcoded-attribute");
        createProtocolMapper(protocolMappersResource, "hardcoded-attribute", "saml", "saml-hardcode-attribute-mapper", config);

        config = new LinkedHashMap<>();
        config.put("role", "hardcoded-role");
        createProtocolMapper(protocolMappersResource, "hardcoded-role", "saml", "saml-hardcode-role-mapper", config);

        config = new LinkedHashMap<>();
        config.put("new.role.name", "pee-on");
        config.put("role", "http://localhost:8081/employee/.employee");
        createProtocolMapper(protocolMappersResource, "renamed-employee-role", "saml", "saml-role-name-mapper", config);

        for (ProtocolMapperRepresentation mapper : clientResource.toRepresentation().getProtocolMappers()) {
            if (mapper.getName().equals("role-list")) {
                protocolMappersResource.delete(mapper.getId());

                mapper.setId(null);
                mapper.getConfig().put(RoleListMapper.SINGLE_ROLE_ATTRIBUTE, "true");
                mapper.getConfig().put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "memberOf");
                protocolMappersResource.createMapper(mapper);
            }
        }

        setRolesToCheck("pee-on,el-jefe,manager,hardcoded-role");

        config = new LinkedHashMap<>();
        config.put("new.role.name", "el-jefe");
        config.put("role", "user");
        createProtocolMapper(protocolMappersResource, "renamed-role", "saml", "saml-role-name-mapper", config);

        employee2ServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login(bburkeUser);

        driver.navigate().to(employee2ServletPage.toString() + "/getAttributes");
        waitUntilElement(By.xpath("//body")).text().contains("hardcoded-attribute: hard");
        employee2ServletPage.checkRolesEndPoint(false);
        employee2ServletPage.logout();
        checkLoggedOut(employee2ServletPage, testRealmSAMLPostLoginPage);
    }

    private void createProtocolMapper(ProtocolMappersResource resource, String name, String protocol, String protocolMapper, Map<String, String> config) {
        ProtocolMapperRepresentation representation = new ProtocolMapperRepresentation();
        representation.setName(name);
        representation.setProtocol(protocol);
        representation.setProtocolMapper(protocolMapper);
        representation.setConfig(config);
        resource.createMapper(representation);
    }

    private void setRolesToCheck(String roles) {
        employee2ServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        driver.navigate().to(employee2ServletPage.toString() + "/setCheckRoles?roles=" + roles);
        employee2ServletPage.logout();
    }

    private void assertOnForbiddenPage() {
        switch (System.getProperty("app.server")) {
            case "eap6":
                waitUntilElement(By.xpath("//body")).text().not().contains("principal=");
                String source = driver.getPageSource();
                assertTrue(source.isEmpty() || source.contains("<body></body>"));
                break;
            default:
                waitUntilElement(By.xpath("//body")).text().contains(FORBIDDEN_TEXT);
        }
    }
}
