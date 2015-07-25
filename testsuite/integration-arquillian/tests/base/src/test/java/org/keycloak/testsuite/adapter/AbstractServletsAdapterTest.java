package org.keycloak.testsuite.adapter;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.testsuite.adapter.servlet.CallAuthenticatedServlet;
import org.keycloak.testsuite.adapter.servlet.CustomerDatabaseServlet;
import org.keycloak.testsuite.adapter.servlet.CustomerServlet;
import org.keycloak.testsuite.adapter.servlet.ErrorServlet;
import org.keycloak.testsuite.adapter.servlet.InputServlet;
import org.keycloak.testsuite.adapter.servlet.ProductServlet;
import org.keycloak.testsuite.adapter.servlet.SessionServlet;
import org.keycloak.testsuite.account.page.Sessions;
import static org.junit.Assert.*;
import org.keycloak.Version;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.util.RealmUtils.*;
import org.keycloak.testsuite.arquillian.jira.Jira;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrl;
import org.keycloak.testsuite.adapter.page.CustomerDb;
import org.keycloak.testsuite.adapter.page.CustomerDbErrorPage;
import org.keycloak.testsuite.adapter.page.CustomerPortal;
import org.keycloak.testsuite.adapter.page.InputPortal;
import org.keycloak.testsuite.adapter.page.ProductPortal;
import org.keycloak.testsuite.adapter.page.SecurePortal;
import org.keycloak.testsuite.adapter.page.SessionPortal;
import static org.keycloak.testsuite.console.page.Realm.DEMO;
import org.keycloak.testsuite.page.auth.Login;
import org.keycloak.testsuite.util.SeleniumUtils;
import org.keycloak.testsuite.util.ApiUtil;
import static org.keycloak.testsuite.util.RealmAssert.assertCurrentUrlStartsWithLoginUrlOf;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.Time;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public abstract class AbstractServletsAdapterTest extends AbstractAdapterTest {

    @Page
    private CustomerPortal customerPortal;
    @Page
    private SecurePortal securePortal;
    @Page
    private CustomerDb customerDb;
    @Page
    private CustomerDbErrorPage customerDbErrorPage;
    @Page
    private ProductPortal productPortal;
    @Page
    private SessionPortal sessionPortal;
    @Page
    private InputPortal inputPortal;

    @Page
    private Sessions accountSessionsPage;

    protected static WebArchive servletDeployment(String name, Class... servletClasses) {
        return servletDeployment(name, "keycloak.json", servletClasses);
    }

    protected static WebArchive servletDeployment(String name, String adapterConfig, Class... servletClasses) {
        String webInfPath = "/adapter-test/" + name + "/WEB-INF/";

        URL keycloakJSON = AbstractServletsAdapterTest.class.getResource(webInfPath + adapterConfig);
        URL webXML = AbstractServletsAdapterTest.class.getResource(webInfPath + "web.xml");

        WebArchive deployment = ShrinkWrap.create(WebArchive.class, name + ".war")
                .addClasses(servletClasses)
                .addAsWebInfResource(webXML, "web.xml")
                .addAsWebInfResource(keycloakJSON, "keycloak.json")
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);

        addContextXml(deployment, name);

        return deployment;
    }

    @Deployment(name = CustomerPortal.DEPLOYMENT_NAME)
    protected static WebArchive customerPortal() {
        return AbstractServletsAdapterTest.servletDeployment(CustomerPortal.DEPLOYMENT_NAME, CustomerServlet.class, ErrorServlet.class);
    }

    @Deployment(name = SecurePortal.DEPLOYMENT_NAME)
    protected static WebArchive securePortal() {
        return AbstractServletsAdapterTest.servletDeployment(SecurePortal.DEPLOYMENT_NAME, CallAuthenticatedServlet.class);
    }

    @Deployment(name = CustomerDb.DEPLOYMENT_NAME)
    protected static WebArchive customerDb() {
        return AbstractServletsAdapterTest.servletDeployment(CustomerDb.DEPLOYMENT_NAME, CustomerDatabaseServlet.class);
    }

    @Deployment(name = CustomerDbErrorPage.DEPLOYMENT_NAME)
    protected static WebArchive customerDbErrorPage() {
        return AbstractServletsAdapterTest.servletDeployment(CustomerDbErrorPage.DEPLOYMENT_NAME, CustomerDatabaseServlet.class, ErrorServlet.class);
    }

    @Deployment(name = ProductPortal.DEPLOYMENT_NAME)
    protected static WebArchive productPortal() {
        return AbstractServletsAdapterTest.servletDeployment(ProductPortal.DEPLOYMENT_NAME, ProductServlet.class);
    }

    @Deployment(name = SessionPortal.DEPLOYMENT_NAME)
    protected static WebArchive sessionPortal() {
        return servletDeployment(SessionPortal.DEPLOYMENT_NAME, "keycloak.json", SessionServlet.class);
    }

    @Deployment(name = InputPortal.DEPLOYMENT_NAME)
    protected static WebArchive inputPortal() {
        return servletDeployment(InputPortal.DEPLOYMENT_NAME, "keycloak.json", InputServlet.class);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/demorealm.json"));
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealm.setConsoleRealm(DEMO);
    }

    private final String slash = "";

    @Test
    public void testSavedPostRequest() throws InterruptedException {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to(inputPortal.toString());
        assertEquals(driver.getCurrentUrl(), inputPortal + slash);
        inputPortal.execute("hello");

        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        loginPage.login("bburke@redhat.com", "password");
        assertEquals(driver.getCurrentUrl(), inputPortal + "/secured/post");
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("parameter=hello"));

        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServer.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, customerPortal.toString())
                .build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

        // test unsecured POST KEYCLOAK-901
        Client client = ClientBuilder.newClient();
        Form form = new Form();
        form.param("parameter", "hello");
        String text = client.target(inputPortal + "/unsecured").request().post(Entity.form(form), String.class);
        assertTrue(text.contains("parameter=hello"));
        client.close();
    }

    @Test
    public void testLoginSSOAndLogout() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        SeleniumUtils.waitGuiForElement(loginPage.getUsernameInput());
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        loginPage.login("bburke@redhat.com", "password");
        assertEquals(driver.getCurrentUrl(), customerPortal + slash);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        productPortal.navigateTo();
        assertEquals(productPortal + slash, driver.getCurrentUrl());
        pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

        // View stats
        List<Map<String, String>> stats = keycloak.realm("demo").getClientSessionStats();
        Map<String, String> customerPortalStats = null;
        Map<String, String> productPortalStats = null;
        for (Map<String, String> s : stats) {
            switch (s.get("clientId")) {
                case "customer-portal":
                    customerPortalStats = s;
                    break;
                case "product-portal":
                    productPortalStats = s;
                    break;
            }
        }
        assertEquals(1, Integer.parseInt(customerPortalStats.get("active")));
        assertEquals(1, Integer.parseInt(productPortalStats.get("active")));

        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServer.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, customerPortal).build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        loginPage.cancel();
        assertTrue(driver.getPageSource().contains("Error Page"));
    }

    @Test
    public void testServletRequestLogout() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        loginPage.login("bburke@redhat.com", "password");
        assertEquals(driver.getCurrentUrl(), customerPortal + slash);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        productPortal.navigateTo();
        Assert.assertEquals(driver.getCurrentUrl(), productPortal + slash);
        pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

        // back
        customerPortal.navigateTo();
        assertEquals(driver.getCurrentUrl(), customerPortal + slash);
        pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));
        // test logout

        driver.navigate().to(customerPortal + "/logout");
        assertTrue(driver.getPageSource().contains("servlet logout ok"));

        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
    }

    @Test
    public void testLoginSSOIdle() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        SeleniumUtils.waitGuiForElement(loginPage.getUsernameInput());
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        loginPage.login("bburke@redhat.com", "password");
        assertEquals(driver.getCurrentUrl(), customerPortal + slash);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        RealmRepresentation demoRealm = keycloak.realm("demo").toRepresentation();
        int originalIdle = demoRealm.getSsoSessionIdleTimeout();
        demoRealm.setSsoSessionIdleTimeout(1);
        keycloak.realm("demo").update(demoRealm);

//		Thread.sleep(2000);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

        demoRealm.setSsoSessionIdleTimeout(originalIdle);
        keycloak.realm("demo").update(demoRealm);
    }

    @Test
    @Jira(value = "KEYCLOAK-1478")
    public void testLoginSSOIdleRemoveExpiredUserSessions() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        System.out.println("Current url: " + driver.getCurrentUrl());
        SeleniumUtils.waitGuiForElement(loginPage.getUsernameInput());
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        assertCurrentUrl(customerPortal);
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        RealmRepresentation demoRealm = keycloak.realm("demo").toRepresentation();
        int originalIdle = demoRealm.getSsoSessionIdleTimeout();
        demoRealm.setSsoSessionIdleTimeout(1);
//        keycloak.realm("demo").update(demoRealm);

        Time.setOffset(2);

        // FIXME
        // KEYCLOAK-1478 - no REST API alternative for removeExpiredUserSessions()
        
        // test SSO
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

        // need to cleanup so other tests don't fail, so invalidate http sessions on remote clients.
        demoRealm.setSsoSessionIdleTimeout(originalIdle);
        // note: sessions invalidated after each test, see: AbstractKeycloakTest.afterAbstractKeycloakTest()

        Time.setOffset(0);
    }

    @Test
    public void testLoginSSOMax() throws InterruptedException {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        SeleniumUtils.waitGuiForElement(loginPage.getUsernameInput());
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        loginPage.login("bburke@redhat.com", "password");
        assertEquals(driver.getCurrentUrl(), customerPortal + slash);
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        RealmRepresentation demoRealm = keycloak.realm("demo").toRepresentation();
        int originalIdle = demoRealm.getSsoSessionMaxLifespan();
        demoRealm.setSsoSessionMaxLifespan(1);
        keycloak.realm("demo").update(demoRealm);

        TimeUnit.SECONDS.sleep(2);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

        demoRealm.setSsoSessionIdleTimeout(originalIdle);
        keycloak.realm("demo").update(demoRealm);
    }

    @Jira("KEYCLOAK-518")
    @Test
    public void testNullBearerToken() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(customerDb.toString());
        Response response = target.request().get();
        assertEquals(401, response.getStatus());
        response.close();
        response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer null").get();
        assertEquals(401, response.getStatus());
        response.close();
        client.close();
    }

    @Jira("KEYCLOAK-1368")
    @Test
    public void testNullBearerTokenCustomErrorPage() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(customerDbErrorPage.toString());
        Response response = target.request().get();

        // TODO: follow redirects automatically if possible
        if (response.getStatus() == 302) {
            String location = response.getHeaderString(HttpHeaders.LOCATION);
            response.close();
            response = client.target(location).request().get();
        }
        assertEquals(200, response.getStatus());
        String errorPageResponse = response.readEntity(String.class);
        assertTrue(errorPageResponse.contains("Error Page"));
        response.close();

        response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer null").get();
        // TODO: follow redirects automatically if possible
        if (response.getStatus() == 302) {
            String location = response.getHeaderString(HttpHeaders.LOCATION);
            response.close();
            response = client.target(location).request().get();
        }
        assertEquals(200, response.getStatus());
        errorPageResponse = response.readEntity(String.class);
        assertTrue(errorPageResponse.contains("Error Page"));
        response.close();

        client.close();
    }

    @Jira("KEYCLOAK-518")
    @Test
    public void testBadUser() {
        Client client = ClientBuilder.newClient();
        URI uri = OIDCLoginProtocolService.tokenUrl(authServer.createUriBuilder()).build("demo");
        WebTarget target = client.target(uri);
        String header = BasicAuthHelper.createHeader("customer-portal", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", "monkey@redhat.com")
                .param("password", "password");
        Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
        assertEquals(401, response.getStatus());
        response.close();
        client.close();

    }

    @Test
    public void testVersion() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(authServer.createUriBuilder()).path("version");
        Version version = target.request().get(Version.class);
        assertNotNull(version);
        assertNotNull(version.getVersion());
        assertNotNull(version.getBuildTime());
        assertNotEquals(version.getVersion(), Version.UNKNOWN);
        assertNotEquals(version.getBuildTime(), Version.UNKNOWN);

        Version version2 = client.target(securePortal.toString()).path(AdapterConstants.K_VERSION).request().get(Version.class);
        assertNotNull(version2);
        assertNotNull(version2.getVersion());
        assertNotNull(version2.getBuildTime());
        assertEquals(version.getVersion(), version2.getVersion());
        assertEquals(version.getBuildTime(), version2.getBuildTime());
        client.close();
    }

    @Test
    public void testAuthenticated() {
        // test login to customer-portal which does a bearer request to customer-db
        securePortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        loginPage.login("bburke@redhat.com", "password");
        Assert.assertEquals(driver.getCurrentUrl(), securePortal + slash);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServer.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, securePortal).build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        securePortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
    }

    @Drone
    @SecondBrowser
    protected WebDriver driver2;    
    
    @Jira("KEYCLOAK-732")
    @Test
    public void testSingleSessionInvalidated() {

        loginAndCheckSession(driver, loginPage);

        // cannot pass to loginAndCheckSession becayse loginPage is not working together with driver2, therefore copypasta
        driver2.navigate().to(sessionPortal.toString());
        assertCurrentUrlStartsWithLoginUrlOf(driver2, testRealm);
        driver2.findElement(By.id("username")).sendKeys("bburke@redhat.com");
        driver2.findElement(By.id("password")).sendKeys("password");
        driver2.findElement(By.id("password")).submit();
        assertCurrentUrl(driver2, sessionPortal.toString());
        String pageSource = driver2.getPageSource();
        assertTrue(pageSource.contains("Counter=1"));
        // Counter increased now
        driver2.navigate().to(sessionPortal.toString());
        pageSource = driver2.getPageSource();
        assertTrue(pageSource.contains("Counter=2"));

        // Logout in browser1
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServer.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, sessionPortal).build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

        // Assert that I am logged out in browser1
        sessionPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

        // Assert that I am still logged in browser2 and same session is still preserved
        driver2.navigate().to(sessionPortal.toString());
        assertCurrentUrl(driver2, sessionPortal.toString());
        pageSource = driver2.getPageSource();
        assertTrue(pageSource.contains("Counter=3"));

        driver2.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(driver2, testRealm);

    }

    @Test
    @Jira("KEYCLOAK-741, KEYCLOAK-1485")
    public void testSessionInvalidatedAfterFailedRefresh() {
        RealmResource demoRealm = keycloak.realm("demo");
        RealmRepresentation demoRealmRep = demoRealm.toRepresentation();
        ClientResource sessionPortalRes = null;
        for (ClientRepresentation clientRep : demoRealm.clients().findAll()) {
            if ("session-portal".equals(clientRep.getClientId())) {
                sessionPortalRes = demoRealm.clients().get(clientRep.getId());
            }
        }
        assertNotNull(sessionPortalRes);
        sessionPortalRes.toRepresentation().setAdminUrl("");
        int origTokenLifespan = demoRealmRep.getAccessCodeLifespan();
        demoRealmRep.setAccessCodeLifespan(1);
        demoRealm.update(demoRealmRep);

        // Login
        loginAndCheckSession(driver, loginPage);

        // Logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServer.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, this.sessionPortal).build("demo").toString();
        driver.navigate().to(logoutUri);

        // Assert that http session was invalidated
        sessionPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        loginPage.login("bburke@redhat.com", "password");
        assertEquals(driver.getCurrentUrl(), this.sessionPortal + slash);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Counter=1"));

        sessionPortalRes.toRepresentation().setAdminUrl(this.sessionPortal.toString());
        demoRealmRep.setAccessCodeLifespan(origTokenLifespan);
        demoRealm.update(demoRealmRep);
    }

    @Test
    @Jira("KEYCLOAK-942")
    public void testAdminApplicationLogout() {
        // login as bburke
        loginAndCheckSession(driver, loginPage);
        // logout mposolda with admin client
        ApiUtil.findClientByClientId(keycloak.realm("demo"), "session-portal").logoutUser("mposolda");
        // bburke should be still logged with original httpSession in our browser window
        sessionPortal.navigateTo();
        assertEquals(driver.getCurrentUrl(), sessionPortal + slash);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Counter=3"));
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServer.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, sessionPortal).build("demo").toString();
        driver.navigate().to(logoutUri);
    }

    @Test
    @Jira("KEYCLOAK-1216, KEYCLOAK-1485")
    public void testAccountManagementSessionsLogout() {
        // login as bburke
        loginAndCheckSession(driver, loginPage);
        loginAsAdmin();
        testRealm.navigateTo();
        menuPage.goToAccountManagement();
        accountSessionsPage.sessions();
        accountSessionsPage.logoutAll();
        // Assert I need to login again (logout was propagated to the app)
        loginAndCheckSession(driver, loginPage);
    }

    private void loginAndCheckSession(WebDriver driver, Login loginPage) {
        sessionPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
        loginPage.login("bburke@redhat.com", "password");
        assertEquals(driver.getCurrentUrl(), sessionPortal + slash);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Counter=1"));

        // Counter increased now
        sessionPortal.navigateTo();
        pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Counter=2"));
    }

}
