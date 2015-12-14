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
import org.keycloak.testsuite.util.IOUtil;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import java.util.List;

import static com.mongodb.util.MyAsserts.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
        testRealmSAMLLoginPage.setAuthRealm(SAMLSERVLETDEMO);
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
        testRealmSAMLLoginPage.form().login("unauthorized", "password");

        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));

        employee2ServletPage.navigateTo();
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));

        employeeSigFrontServletPage.navigateTo();
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));

        salesPostSigPersistentServletPage.navigateTo();
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));

        salesPostServletPage.logout();
    }

    @Test
    public void singleLoginAndLogoutSAMLTest() {
        salesPostServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostSigServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        employee2ServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostEncServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        employeeSigFrontServletPage.logout();

        employeeSigFrontServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);

        employeeSigServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);

        salesPostPassiveServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("Forbidden"));

        salesPostSigEmailServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);
    }

    @Test
    public void badClientSalesPostSigTest() {
        badClientSalesPostSigServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("invalidRequesterMessage"));
    }

    @Test
    public void badRealmSalesPostSigTest() {
        badRealmSalesPostSigServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("Forbidden"));
    }

    @Test
    public void employee2Test() {
        employee2ServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        employee2ServletPage.logout();
        employee2ServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);

        testRealmSAMLLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));
        employee2ServletPage.logout();
    }

    @Test
    public void employeeSigTest() {
        employeeSigServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        employeeSigServletPage.logout();
        employeeSigServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);

        testRealmSAMLLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));
        employeeSigServletPage.logout();
    }

    @Test
    public void employeeSigFrontTest() {
        employeeSigFrontServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        employeeSigFrontServletPage.logout();
        employeeSigFrontServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);

        testRealmSAMLLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));
        employeeSigFrontServletPage.logout();
    }

    @Test
    public void salesMetadataTest() throws Exception {
        Document doc = loadXML(AbstractSAMLServletsAdapterTest.class.getResourceAsStream("/adapter-test/keycloak-saml/sp-metadata.xml"));

        modifyDocElementAttribute(doc, "SingleLogoutService", "Location", "8080", System.getProperty("auth.server.http.port", null));
        modifyDocElementAttribute(doc, "AssertionConsumerService", "Location", "8080", System.getProperty("auth.server.http.port", null));

        ClientRepresentation clientRep = testRealmResource().convertClientDescription(IOUtil.documentToString(doc));
        Response response = testRealmResource().clients().create(clientRep);
        assertEquals(201, response.getStatus());

        salesMetadataServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesMetadataServletPage.logout();
        salesMetadataServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);

        testRealmSAMLLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));
        salesMetadataServletPage.logout();
    }

    @Test
    public void salesPostTest() {
        salesPostServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostServletPage.logout();
        salesPostServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);

        testRealmSAMLLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));
        salesPostServletPage.logout();
    }

    @Test
    public void salesPostEncTest() {
        salesPostEncServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostEncServletPage.logout();
        salesPostEncServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);

        testRealmSAMLLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));
        salesPostEncServletPage.logout();
    }

    @Test
    public void salesPostPassiveTest() {
        salesPostPassiveServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("Forbidden"));

        salesPostServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);

        salesPostPassiveServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostPassiveServletPage.logout();
        salesPostPassiveServletPage.navigateTo();
        assertTrue(driver.getPageSource().contains("Forbidden"));

        salesPostServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login("unauthorized", "password");
        salesPostPassiveServletPage.navigateTo();
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));
        salesPostPassiveServletPage.logout();
    }

    @Test
    public void salesPostSigTest() {
        salesPostEncServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostEncServletPage.logout();
        salesPostEncServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);

        testRealmSAMLLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));
        salesPostEncServletPage.logout();
    }

    @Test
    public void salesPostSigEmailTest() {
        salesPostSigEmailServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertTrue(driver.getPageSource().contains("principal=bburke"));

        salesPostSigEmailServletPage.logout();
        salesPostSigEmailServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);

        testRealmSAMLLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));
        salesPostSigEmailServletPage.logout();
    }

    @Test
    public void salesPostSigPersistentTest() {
        salesPostSigPersistentServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertFalse(driver.getPageSource().contains("bburke"));
        assertTrue(driver.getPageSource().contains("principal=G-"));

        salesPostSigPersistentServletPage.logout();
        salesPostSigPersistentServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);

        testRealmSAMLLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));
        salesPostSigPersistentServletPage.logout();
    }

    @Test
    public void salesPostSigTransientTest() {
        salesPostSigTransientServletPage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);
        assertFalse(driver.getPageSource().contains("bburke"));
        assertTrue(driver.getPageSource().contains("principal=G-"));

        salesPostSigTransientServletPage.logout();
        salesPostSigTransientServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLLoginPage);

        testRealmSAMLLoginPage.form().login("unauthorized", "password");
        assertFalse(driver.getPageSource().contains("principal="));
        assertTrue(driver.getPageSource().contains("Forbidden"));
        salesPostSigTransientServletPage.logout();
    }
}
