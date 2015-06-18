package org.keycloak.testsuite.adapter;

import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.testsuite.adapter.servlet.CallAuthenticatedServlet;
import org.keycloak.testsuite.adapter.servlet.CustomerDatabaseServlet;
import org.keycloak.testsuite.adapter.servlet.CustomerServlet;
import org.keycloak.testsuite.adapter.servlet.ErrorServlet;
import org.keycloak.testsuite.adapter.servlet.InputServlet;
import org.keycloak.testsuite.adapter.servlet.ProductServlet;
import org.keycloak.testsuite.adapter.servlet.SessionServlet;
import org.keycloak.testsuite.ui.application.page.InputPage;

public abstract class AbstractServletsAdapterTest extends AbstractAdapterTest {

    public static final String CUSTOMER_PORTAL = "customer-portal";
    public static final String SECURE_PORTAL = "secure-portal";
    public static final String CUSTOMER_DB = "customer-db";
    public static final String CUSTOMER_DB_ERROR_PAGE = "customer-db-error-page";
    public static final String PRODUCT_PORTAL = "product-portal";
    public static final String SESSION_PORTAL = "session-portal";
    public static final String INPUT_PORTAL = "input-portal";

    @Page
    private InputPage inputPage;

    public AbstractServletsAdapterTest(String appServerBaseURL) {
        super(appServerBaseURL);
    }

    protected static WebArchive adapterDeployment(String name, Class... servletClasses) {
        return adapterDeployment(name, "keycloak.json", servletClasses);
    }

    protected static WebArchive adapterDeployment(String name, String adapterConfig, Class... servletClasses) {
        String webInfPath = "/adapter-test/" + name + "/WEB-INF/";
        URL keycloakJSON = AbstractServletsAdapterTest.class.getResource(webInfPath + adapterConfig);
        URL webXML = AbstractServletsAdapterTest.class.getResource(webInfPath + "web.xml");
        WebArchive deployment = ShrinkWrap.create(WebArchive.class, name + ".war")
                .addClasses(servletClasses)
                .addAsWebInfResource(webXML, "web.xml")
                .addAsWebInfResource(keycloakJSON, "keycloak.json");

        URL jbossDeploymentStructure = AbstractServletsAdapterTest.class.getResource(webInfPath + "jboss-deployment-structure.xml");
        if (jbossDeploymentStructure != null) {
            deployment = deployment.addAsWebInfResource(jbossDeploymentStructure, "jboss-deployment-structure.xml");
        }

        return deployment;
    }

    @Deployment(name = CUSTOMER_PORTAL, managed = false)
    private static WebArchive customerPortal() {
        return adapterDeployment(CUSTOMER_PORTAL, CustomerServlet.class, ErrorServlet.class);
    }

    @Deployment(name = SECURE_PORTAL, managed = false)
    private static WebArchive securePortal() {
        return adapterDeployment(SECURE_PORTAL, CallAuthenticatedServlet.class);
    }

    @Deployment(name = CUSTOMER_DB, managed = false)
    private static WebArchive customerDb() {
        return adapterDeployment(CUSTOMER_DB, CustomerDatabaseServlet.class);
    }

    @Deployment(name = CUSTOMER_DB_ERROR_PAGE, managed = false)
    private static WebArchive customerDbErrorPage() {
        return adapterDeployment(CUSTOMER_DB_ERROR_PAGE, CustomerDatabaseServlet.class, ErrorServlet.class);
    }

    @Deployment(name = PRODUCT_PORTAL, managed = false)
    private static WebArchive productPortal() {
        return adapterDeployment(PRODUCT_PORTAL, ProductServlet.class);
    }

    @Deployment(name = SESSION_PORTAL, managed = false)
    private static WebArchive sessionPortal() {
        return adapterDeployment(SESSION_PORTAL, "keycloak.json", SessionServlet.class);
    }

    @Deployment(name = INPUT_PORTAL, managed = false)
    private static WebArchive inputPortal() {
        return adapterDeployment(INPUT_PORTAL, "keycloak.json", InputServlet.class);
    }

    private final String slash = "";

//    @Test
//    @InSequence(-1)
//    @OperateOnDeployment(CUSTOMER_PORTAL)
//    public void testCustomerPortalURLInjection(@InitialPage CustomerPortalPage customerPortalPage) throws InterruptedException {
//        customerPortalPage.customerListing();
//        Thread.sleep(60000);
//    }

    @Test
    public void testSavedPostRequest() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to(APP_SERVER_BASE_URL + "/input-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/input-portal" + slash);
        inputPage.execute("hello");

        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/input-portal/secured/post");
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("parameter=hello"));

        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
                .queryParam(OAuth2Constants.REDIRECT_URI, APP_SERVER_BASE_URL + "/customer-portal").build("demo").toString();
        driver.navigate().to(logoutUri);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

        // test unsecured POST KEYCLOAK-901
        Client client = ClientBuilder.newClient();
        Form form = new Form();
        form.param("parameter", "hello");
        String text = client.target(APP_SERVER_BASE_URL + "/input-portal/unsecured").request().post(Entity.form(form), String.class);
        Assert.assertTrue(text.contains("parameter=hello"));
        client.close();
    }

    @Test
    public void testLoginSSOAndLogout() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/customer-portal" + slash);
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/product-portal" + slash);
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

        // View stats
        List<Map<String, String>> stats = Keycloak.getInstance(AUTH_SERVER_URL,
                "master", "admin", "admin", "security-admin-console").realm("demo").getClientSessionStats();
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
        Assert.assertEquals(1, Integer.parseInt(customerPortalStats.get("active")));
        Assert.assertEquals(1, Integer.parseInt(productPortalStats.get("active")));

        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
                .queryParam(OAuth2Constants.REDIRECT_URI, APP_SERVER_BASE_URL + "/customer-portal").build("demo").toString();
        driver.navigate().to(logoutUri);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.cancel();
        System.out.println(driver.getPageSource());
        Assert.assertTrue(driver.getPageSource().contains("Error Page"));
    }

    @Test
    public void testServletRequestLogout() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/customer-portal" + slash);
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/product-portal" + slash);
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

        // back
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/customer-portal" + slash);
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));
        // test logout

        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal/logout");
        Assert.assertTrue(driver.getPageSource().contains("servlet logout ok"));

        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.startsWith(LOGIN_URL));
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

    }

    // TODO remaining adapter tests here
}
