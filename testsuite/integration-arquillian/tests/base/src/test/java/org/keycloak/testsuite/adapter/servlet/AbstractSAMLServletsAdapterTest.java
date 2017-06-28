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

import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.KeyProvider;
import org.keycloak.keys.ImportedRsaKeyProviderFactory;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.RoleListMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2ErrorResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.XmlKeyInfoKeyNameTransformer;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.*;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.login.Login;
import org.keycloak.testsuite.auth.page.login.SAMLIDPInitiatedLogin;
import org.keycloak.testsuite.page.AbstractPage;
import org.keycloak.testsuite.util.*;

import org.keycloak.testsuite.util.SamlClient.Binding;
import org.openqa.selenium.By;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.ApiUtil.createUserAndResetPasswordWithAdminClient;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.auth.page.AuthRealm.SAMLSERVLETDEMO;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.IOUtil.loadXML;
import static org.keycloak.testsuite.util.IOUtil.modifyDocElementAttribute;
import static org.keycloak.testsuite.util.Matchers.bodyHC;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;
import static org.keycloak.testsuite.util.SamlClient.idpInitiatedLogin;
import static org.keycloak.testsuite.util.SamlClient.login;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.*;

/**
 * @author mhajas
 */
public abstract class AbstractSAMLServletsAdapterTest extends AbstractServletsAdapterTest {
    @Page
    protected BadClientSalesPostSigServlet badClientSalesPostSigServletPage;

    @Page
    protected BadRealmSalesPostSigServlet badRealmSalesPostSigServletPage;

    @Page
    protected EmployeeAcsServlet employeeAcsServletPage;

    @Page
    protected Employee2Servlet employee2ServletPage;

    @Page
    protected EmployeeSigServlet employeeSigServletPage;

    @Page
    protected EmployeeSigPostNoIdpKeyServlet employeeSigPostNoIdpKeyServletPage;

    @Page
    protected EmployeeSigRedirNoIdpKeyServlet employeeSigRedirNoIdpKeyServletPage;

    @Page
    protected EmployeeSigRedirOptNoIdpKeyServlet employeeSigRedirOptNoIdpKeyServletPage;

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
    protected SalesPostEncSignAssertionsOnlyServlet salesPostEncSignAssertionsOnlyServletPage;

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
    protected DifferentCookieNameServlet differentCookieNameServletPage;

    @Page
    private InputPortal inputPortalPage;

    @Page
    private SAMLIDPInitiatedLogin samlidpInitiatedLoginPage;

    @Page
    protected SalesPostAutodetectServlet salesPostAutodetectServletPage;

    public static final String FORBIDDEN_TEXT = "HTTP status code: 403";

    @Deployment(name = BadClientSalesPostSigServlet.DEPLOYMENT_NAME)
    protected static WebArchive badClientSalesPostSig() {
        return samlServletDeployment(BadClientSalesPostSigServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = BadRealmSalesPostSigServlet.DEPLOYMENT_NAME)
    protected static WebArchive badRealmSalesPostSig() {
        return samlServletDeployment(BadRealmSalesPostSigServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = EmployeeAcsServlet.DEPLOYMENT_NAME)
    protected static WebArchive employeeAssertionConsumerServiceUrlSet() {
        return samlServletDeployment(EmployeeAcsServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = Employee2Servlet.DEPLOYMENT_NAME)
    protected static WebArchive employee2() {
        return samlServletDeployment(Employee2Servlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = EmployeeSigServlet.DEPLOYMENT_NAME)
    protected static WebArchive employeeSig() {
        return samlServletDeployment(EmployeeSigServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = EmployeeSigPostNoIdpKeyServlet.DEPLOYMENT_NAME)
    protected static WebArchive employeeSigPostNoIdpKeyServlet() {
        return samlServletDeployment(EmployeeSigPostNoIdpKeyServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = EmployeeSigRedirNoIdpKeyServlet.DEPLOYMENT_NAME)
    protected static WebArchive employeeSigRedirNoIdpKeyServlet() {
        return samlServletDeployment(EmployeeSigRedirNoIdpKeyServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = EmployeeSigRedirOptNoIdpKeyServlet.DEPLOYMENT_NAME)
    protected static WebArchive employeeSigRedirOptNoIdpKeyServlet() {
        return samlServletDeployment(EmployeeSigRedirOptNoIdpKeyServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
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

    @Deployment(name = SalesPostEncSignAssertionsOnlyServlet.DEPLOYMENT_NAME)
    protected static WebArchive salesPostEncSignAssertionsOnly() {
        return samlServletDeployment(SalesPostEncSignAssertionsOnlyServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
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
        return samlServletDeployment(InputPortal.DEPLOYMENT_NAME, "input-portal/WEB-INF/web.xml" , InputServlet.class, ServletTestUtils.class);
    }

    @Deployment(name = SalesPost2Servlet.DEPLOYMENT_NAME)
    protected static WebArchive salesPost2() {
        return samlServletDeployment(SalesPost2Servlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = DifferentCookieNameServlet.DEPLOYMENT_NAME)
    protected static WebArchive differentCokieName() {
        return samlServletDeployment(DifferentCookieNameServlet.DEPLOYMENT_NAME, "different-cookie-name/WEB-INF/web.xml", SendUsernameServlet.class);
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
        return samlServletDeployment(EmployeeServlet.DEPLOYMENT_NAME, "employee/WEB-INF/web.xml", SamlSPFacade.class, ServletTestUtils.class);
    }

    @Deployment(name = SalesPostAutodetectServlet.DEPLOYMENT_NAME)
    protected static WebArchive salesPostAutodetect() {
        return samlServletDeployment(SalesPostAutodetectServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
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
        waitForPageToLoad(driver);
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
    public void employeeAcsTest() {
        SAMLDocumentHolder samlResponse = new SamlClient(employeeAcsServletPage.buildUri()).getSamlResponse(Binding.POST, (client, context, strategy) -> {
            strategy.setRedirectable(false);
            return client.execute(new HttpGet(employeeAcsServletPage.buildUri()), context);
        });

        assertThat(samlResponse.getSamlObject(), instanceOf(AuthnRequestType.class));
        assertThat(((AuthnRequestType) samlResponse.getSamlObject()).getAssertionConsumerServiceURL(), notNullValue());
        assertThat(((AuthnRequestType) samlResponse.getSamlObject()).getAssertionConsumerServiceURL().getPath(), is("/employee-acs/a/different/endpoint/for/saml"));

        assertSuccessfulLogin(employeeAcsServletPage, bburkeUser, testRealmSAMLPostLoginPage, "principal=bburke");
    }

    private static final KeyPair NEW_KEY_PAIR = KeyUtils.generateRsaKeyPair(1024);
    private static final String NEW_KEY_PRIVATE_KEY_PEM = PemUtils.encodeKey(NEW_KEY_PAIR.getPrivate());

    private PublicKey createKeys(String priority) throws Exception {
        PublicKey publicKey = NEW_KEY_PAIR.getPublic();

        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("mycomponent");
        rep.setParentId("demo");
        rep.setProviderId(ImportedRsaKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());

        org.keycloak.common.util.MultivaluedHashMap config = new org.keycloak.common.util.MultivaluedHashMap();
        config.addFirst("priority", priority);
        config.addFirst(Attributes.PRIVATE_KEY_KEY, NEW_KEY_PRIVATE_KEY_PEM);
        rep.setConfig(config);

        testRealmResource().components().add(rep);

        return publicKey;
    }

    private void dropKeys(String priority) {
        for (ComponentRepresentation c : testRealmResource().components().query("demo", KeyProvider.class.getName())) {
            if (c.getConfig().getFirst("priority").equals(priority)) {
                testRealmResource().components().component(c.getId()).remove();
                return;
            }
        }
        throw new RuntimeException("Failed to find keys");
    }

    private void testRotatedKeysPropagated(SAMLServlet servletPage, Login loginPage) throws Exception {
        boolean keyDropped = false;
        try {
            log.info("Creating new key");
            createKeys("1000");
            testSuccessfulAndUnauthorizedLogin(servletPage, loginPage);
            log.info("Dropping new key");
            dropKeys("1000");
            keyDropped = true;
            testSuccessfulAndUnauthorizedLogin(servletPage, loginPage);
        } finally {
            if (! keyDropped) {
                dropKeys("1000");
            }
        }
    }

    @Test
    public void employeeSigPostNoIdpKeyTest() throws Exception {
        testRotatedKeysPropagated(employeeSigPostNoIdpKeyServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void employeeSigPostNoIdpKeyTestNoKeyNameInKeyInfo() throws Exception {
        RealmRepresentation r = testRealmResource().toRepresentation();
        r.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE_KEYINFO_KEY_NAME_TRANSFORMER, XmlKeyInfoKeyNameTransformer.NONE.name());
        testRotatedKeysPropagated(employeeSigPostNoIdpKeyServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void employeeSigPostNoIdpKeyTestCertSubjectAsKeyNameInKeyInfo() throws Exception {
        RealmRepresentation r = testRealmResource().toRepresentation();
        r.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE_KEYINFO_KEY_NAME_TRANSFORMER, XmlKeyInfoKeyNameTransformer.CERT_SUBJECT.name());
        testRotatedKeysPropagated(employeeSigPostNoIdpKeyServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void employeeSigPostNoIdpKeyTestKeyIdAsKeyNameInKeyInfo() throws Exception {
        RealmRepresentation r = testRealmResource().toRepresentation();
        r.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE_KEYINFO_KEY_NAME_TRANSFORMER, XmlKeyInfoKeyNameTransformer.KEY_ID.name());
        testRotatedKeysPropagated(employeeSigPostNoIdpKeyServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void employeeSigRedirNoIdpKeyTest() throws Exception {
        testRotatedKeysPropagated(employeeSigRedirNoIdpKeyServletPage, testRealmSAMLRedirectLoginPage);
    }

    @Test
    public void employeeSigRedirNoIdpKeyTestNoKeyNameInKeyInfo() throws Exception {
        RealmRepresentation r = testRealmResource().toRepresentation();
        r.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE_KEYINFO_KEY_NAME_TRANSFORMER, XmlKeyInfoKeyNameTransformer.NONE.name());
        testRotatedKeysPropagated(employeeSigRedirNoIdpKeyServletPage, testRealmSAMLRedirectLoginPage);
    }

    @Test
    public void employeeSigRedirNoIdpKeyTestCertSubjectAsKeyNameInKeyInfo() throws Exception {
        RealmRepresentation r = testRealmResource().toRepresentation();
        r.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE_KEYINFO_KEY_NAME_TRANSFORMER, XmlKeyInfoKeyNameTransformer.CERT_SUBJECT.name());
        testRotatedKeysPropagated(employeeSigRedirNoIdpKeyServletPage, testRealmSAMLRedirectLoginPage);
    }

    @Test
    public void employeeSigRedirNoIdpKeyTestKeyIdAsKeyNameInKeyInfo() throws Exception {
        RealmRepresentation r = testRealmResource().toRepresentation();
        r.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE_KEYINFO_KEY_NAME_TRANSFORMER, XmlKeyInfoKeyNameTransformer.KEY_ID.name());
        testRotatedKeysPropagated(employeeSigRedirNoIdpKeyServletPage, testRealmSAMLRedirectLoginPage);
    }

    @Test
    public void employeeSigRedirOptNoIdpKeyTest() throws Exception {
        testRotatedKeysPropagated(employeeSigRedirOptNoIdpKeyServletPage, testRealmSAMLRedirectLoginPage);
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
    public void salesPostTestCompositeRoleForUser() {
        UserRepresentation topGroupUser = createUserRepresentation("topGroupUser", "top@redhat.com", "", "", true);
        setPasswordFor(topGroupUser, PASSWORD);

        assertSuccessfulLogin(salesPostServletPage, topGroupUser, testRealmSAMLPostLoginPage, "principal=topgroupuser");

        salesPostServletPage.logout();
        checkLoggedOut(salesPostServletPage, testRealmSAMLPostLoginPage);
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
    public void salesPostEncSignedAssertionsOnlyTest() throws Exception {
        testSuccessfulAndUnauthorizedLogin(salesPostEncSignAssertionsOnlyServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    public void salesPostEncSignedAssertionsAndDocumentTest() throws Exception {
        ClientRepresentation salesPostEncClient = testRealmResource().clients().findByClientId(SalesPostEncServlet.CLIENT_NAME).get(0);
        try (Closeable client = new ClientAttributeUpdater(testRealmResource().clients().get(salesPostEncClient.getId()))
          .setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true")
          .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
          .update()) {
            testSuccessfulAndUnauthorizedLogin(salesPostEncServletPage, testRealmSAMLPostLoginPage);
        } finally {
            salesPostEncServletPage.logout();
        }
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
    // https://issues.jboss.org/browse/KEYCLOAK-3971
    public void salesPostSigTestUnicodeCharacters() {
        final String username = "ěščřžýáíRoàåéèíñòøöùüßÅÄÖÜ";
        UserRepresentation user = UserBuilder
          .edit(createUserRepresentation(username, "xyz@redhat.com", "ěščřžýáí", "RoàåéèíñòøöùüßÅÄÖÜ", true))
          .addPassword(PASSWORD)
          .build();
        String userId = createUserAndResetPasswordWithAdminClient(testRealmResource(), user, PASSWORD);
        final RoleScopeResource realmRoleRes = testRealmResource().users().get(userId).roles().realmLevel();
        List<RoleRepresentation> availableRoles = realmRoleRes.listAvailable();
        realmRoleRes.add(availableRoles.stream().filter(r -> r.getName().equalsIgnoreCase("manager")).collect(Collectors.toList()));

        UserRepresentation storedUser = testRealmResource().users().get(userId).toRepresentation();

        assertThat(storedUser, notNullValue());
        assertThat("Database seems to be unable to store Unicode for username. Refer to KEYCLOAK-3439 and related issues.", storedUser.getUsername(), equalToIgnoringCase(username));

        assertSuccessfulLogin(salesPostSigServletPage, user, testRealmSAMLPostLoginPage, "principal=" + storedUser.getUsername());

        salesPostSigServletPage.logout();
        checkLoggedOut(salesPostSigServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    // https://issues.jboss.org/browse/KEYCLOAK-3971
    public void employeeSigTestUnicodeCharacters() {
        final String username = "ěščřžýáíRoàåéèíñòøöùüßÅÄÖÜ";
        UserRepresentation user = UserBuilder
          .edit(createUserRepresentation(username, "xyz@redhat.com", "ěščřžýáí", "RoàåéèíñòøöùüßÅÄÖÜ", true))
          .addPassword(PASSWORD)
          .build();
        String userId = createUserAndResetPasswordWithAdminClient(testRealmResource(), user, PASSWORD);
        final RoleScopeResource realmRoleRes = testRealmResource().users().get(userId).roles().realmLevel();
        List<RoleRepresentation> availableRoles = realmRoleRes.listAvailable();
        realmRoleRes.add(availableRoles.stream().filter(r -> r.getName().equalsIgnoreCase("manager")).collect(Collectors.toList()));

        UserRepresentation storedUser = testRealmResource().users().get(userId).toRepresentation();

        assertThat(storedUser, notNullValue());
        assertThat("Database seems to be unable to store Unicode for username. Refer to KEYCLOAK-3439 and related issues.", storedUser.getUsername(), equalToIgnoringCase(username));

        assertSuccessfulLogin(employeeSigServletPage, user, testRealmSAMLRedirectLoginPage, "principal=" + storedUser.getUsername());

        employeeSigServletPage.logout();
        checkLoggedOut(employeeSigServletPage, testRealmSAMLRedirectLoginPage);
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
    public void idpInitiatedLoginTest() {
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

        waitUntilElement(By.xpath("//body")).text().contains("Error info: SamlAuthenticationError [reason=INVALID_SIGNATURE");
        assertEquals(driver.getCurrentUrl(), badAssertionSalesPostSigPage + "/saml");
    }

    @Test
    public void testMissingAssertionSignature() {
        missingAssertionSigPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login("bburke", "password");

        waitUntilElement(By.xpath("//body")).text().contains("Error info: SamlAuthenticationError [reason=INVALID_SIGNATURE");
        assertEquals(driver.getCurrentUrl(), missingAssertionSigPage + "/saml");
    }

    @Test
    public void testErrorHandlingUnsigned() throws Exception {
        Client client = ClientBuilder.newClient();
        // make sure
        Response response = client.target(employeeServletPage.toString()).request().get();
        response.close();
        SAML2ErrorResponseBuilder builder = new SAML2ErrorResponseBuilder()
                .destination(employeeServletPage.toString() + "/saml")
                .issuer("http://localhost:" + System.getProperty("auth.server.http.port", "8180") + "/realms/demo")
                .status(JBossSAMLURIConstants.STATUS_REQUEST_DENIED.get());
        BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder()
                .relayState(null);
        Document document = builder.buildDocument();
        URI uri = binding.redirectBinding(document).generateURI(employeeServletPage.toString() + "/saml", false);
        response = client.target(uri).request().get();
        String errorPage = response.readEntity(String.class);
        response.close();
        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void testRelayStateEncoding() throws Exception {
        // this test has a hardcoded SAMLRequest and we hack a SP face servlet to get the SAMLResponse so we can look
        // at the relay state
        employeeServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login("bburke", "password");
        assertCurrentUrlStartsWith(employeeServletPage);
        waitForPageToLoad(driver);
        String pageSource = driver.getPageSource();
        assertThat(pageSource, containsString("Relay state: " + SamlSPFacade.RELAY_STATE));
        assertThat(pageSource, not(containsString("SAML response: null")));
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

    @Test
    public void idpMetadataValidation() throws Exception {
        driver.navigate().to(authServerPage.toString() + "/realms/" + SAMLSERVLETDEMO + "/protocol/saml/descriptor");
        validateXMLWithSchema(driver.getPageSource(), "/adapter-test/keycloak-saml/metadata-schema/saml-schema-metadata-2.0.xsd");
    }


    @Test
    public void spMetadataValidation() throws Exception {
        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), "http://localhost:8081/sales-post-sig/");
        ClientRepresentation representation = clientResource.toRepresentation();
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(authServerPage.toString() + "/admin/realms/" + SAMLSERVLETDEMO + "/clients/" + representation.getId() + "/installation/providers/saml-sp-descriptor");
        Response response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.tokenManager().getAccessToken().getToken()).get();
        validateXMLWithSchema(response.readEntity(String.class), "/adapter-test/keycloak-saml/metadata-schema/saml-schema-metadata-2.0.xsd");
        response.close();
    }

    @Test
    //KEYCLOAK-4020
    public void testBooleanAttribute() throws Exception {
        AuthnRequestType req = SamlClient.createLoginRequestDocument("http://localhost:8081/employee2/", getAppServerSamlEndpoint(employee2ServletPage).toString(), getAuthServerSamlEndpoint(SAMLSERVLETDEMO));
        Document doc = SAML2Request.convert(req);

        SAMLDocumentHolder res = login(bburkeUser, getAuthServerSamlEndpoint(SAMLSERVLETDEMO), doc, null, SamlClient.Binding.POST, SamlClient.Binding.POST);
        Document responseDoc = res.getSamlDocument();

        Element attribute = responseDoc.createElement("saml:Attribute");
        attribute.setAttribute("Name", "boolean-attribute");
        attribute.setAttribute("NameFormat", "urn:oasis:names:tc:SAML:2.0:attrname-format:basic");

        Element attributeValue = responseDoc.createElement("saml:AttributeValue");
        attributeValue.setAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema");
        attributeValue.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        attributeValue.setAttribute("xsi:type", "xs:boolean");
        attributeValue.setTextContent("true");

        attribute.appendChild(attributeValue);
        IOUtil.appendChildInDocument(responseDoc, "samlp:Response/saml:Assertion/saml:AttributeStatement", attribute);

        CloseableHttpResponse response = null;
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpClientContext context = HttpClientContext.create();

            HttpUriRequest post = SamlClient.Binding.POST.createSamlUnsignedResponse(getAppServerSamlEndpoint(employee2ServletPage), null, responseDoc);
            response = client.execute(post, context);
            assertThat(response, statusCodeIsHC(Response.Status.FOUND));
            response.close();

            HttpGet get = new HttpGet(employee2ServletPage.toString() + "/getAttributes");
            response = client.execute(get);
            assertThat(response, statusCodeIsHC(Response.Status.OK));
            assertThat(response, bodyHC(containsString("boolean-attribute: true")));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
                try { response.close(); } catch (IOException ex) { }
            }
        }
    }

    // KEYCLOAK-4329
    @Test
    public void testEmptyKeyInfoElement() {
        samlidpInitiatedLoginPage.setAuthRealm(SAMLSERVLETDEMO);
        samlidpInitiatedLoginPage.setUrlName("sales-post-sig-email");
        System.out.println(samlidpInitiatedLoginPage.toString());
        URI idpInitiatedLoginPage = URI.create(samlidpInitiatedLoginPage.toString());

        log.debug("Log in using idp initiated login");
        SAMLDocumentHolder documentHolder = idpInitiatedLogin(bburkeUser, idpInitiatedLoginPage, SamlClient.Binding.POST);


        log.debug("Removing KeyInfo from Keycloak response");
        Document responseDoc = documentHolder.getSamlDocument();
        IOUtil.removeElementFromDoc(responseDoc, "samlp:Response/dsig:Signature/dsig:KeyInfo");

        CloseableHttpResponse response = null;
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpClientContext context = HttpClientContext.create();

            log.debug("Sending response to SP");
            HttpUriRequest post = SamlClient.Binding.POST.createSamlUnsignedResponse(getAppServerSamlEndpoint(salesPostSigEmailServletPage), null, responseDoc);
            response = client.execute(post, context);
            System.out.println(EntityUtils.toString(response.getEntity()));
            assertThat(response, statusCodeIsHC(Response.Status.FOUND));
            response.close();

            HttpGet get = new HttpGet(salesPostSigEmailServletPage.toString());
            response = client.execute(get);
            assertThat(response, statusCodeIsHC(Response.Status.OK));
            assertThat(response, bodyHC(containsString("principal=bburke")));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
                try { response.close(); } catch (IOException ex) { }
            }
        }
    }

    @Test
    // KEYCLOAK-4141
    public void testDifferentCookieName() {
        assertSuccessfulLogin(differentCookieNameServletPage, bburkeUser, testRealmSAMLPostLoginPage, "principal=bburke");

        assertThat(driver.manage().getCookieNamed("DIFFERENT_SESSION_ID"), notNullValue());
        assertThat(driver.manage().getCookieNamed("JSESSIONID"), nullValue());

        salesPost2ServletPage.logout();
        checkLoggedOut(differentCookieNameServletPage, testRealmSAMLPostLoginPage);
    }

    @Test
    /* KEYCLOAK-4980 */
    public void testAutodetectBearerOnly() throws Exception {
        Client client = ClientBuilder.newClient();

        // Do not redirect client to login page if it's an XHR
        WebTarget target = client.target(salesPostAutodetectServletPage.toString());
        Response response = target.request().header("X-Requested-With", "XMLHttpRequest").get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        // Do not redirect client to login page if it's a partial Faces request
        response = target.request().header("Faces-Request", "partial/ajax").get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        // Do not redirect client to login page if it's a SOAP request
        response = target.request().header("SOAPAction", "").get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        // Do not redirect client to login page if Accept header is missing
        response = target.request().get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        // Do not redirect client to login page if client does not understand HTML reponses
        response = target.request().header(HttpHeaders.ACCEPT, "application/json,text/xml").get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        // Redirect client to login page if it's not an XHR
        response = target.request().header("X-Requested-With", "Dont-Know").header(HttpHeaders.ACCEPT, "*/*").get();
        Assert.assertEquals(200, response.getStatus());
        response.close();

        // Redirect client to login page if client explicitely understands HTML responses
        response = target.request().header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9").get();
        Assert.assertEquals(200, response.getStatus());
        response.close();

        // Redirect client to login page if client understands all response types
        response = target.request().header(HttpHeaders.ACCEPT, "*/*").get();
        Assert.assertEquals(200, response.getStatus());
        response.close();
        client.close();
    }

    private URI getAuthServerSamlEndpoint(String realm) throws IllegalArgumentException, UriBuilderException {
        return RealmsResource
                .protocolUrl(UriBuilder.fromUri(getAuthServerRoot()))
                .build(realm, SamlProtocol.LOGIN_PROTOCOL);
    }

    private URI getAppServerSamlEndpoint(SAMLServlet page) throws IllegalArgumentException, UriBuilderException {
        return UriBuilder.fromPath(page.toString()).path("/saml").build();
    }

    private void validateXMLWithSchema(String xml, String schemaFileName) throws SAXException, IOException {
        URL schemaFile = getClass().getResource(schemaFileName);

        Source xmlFile = new StreamSource(new ByteArrayInputStream(xml.getBytes()), xml);
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        try {
            validator.validate(xmlFile);
            System.out.println(xmlFile.getSystemId() + " is valid");
        } catch (SAXException e) {
            System.out.println(xmlFile.getSystemId() + " is NOT valid");
            System.out.println("Reason: " + e.getLocalizedMessage());
            Assert.fail();
        }
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
        waitUntilElement(By.xpath("//body")).text().contains(FORBIDDEN_TEXT);
    }
}
