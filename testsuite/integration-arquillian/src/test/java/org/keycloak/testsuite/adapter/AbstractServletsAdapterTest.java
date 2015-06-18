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
import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
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
import org.keycloak.testsuite.ui.page.account.AccountSessionsPage;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.keycloak.Version;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.ui.page.LoginPage;
import org.keycloak.testsuite.ui.util.SeleniumUtils;
import org.keycloak.testsuite.util.ApiUtil;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.util.BasicAuthHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public abstract class AbstractServletsAdapterTest extends AbstractAdapterTest {

	public static final String CUSTOMER_PORTAL = "customer-portal";
	public static final String SECURE_PORTAL = "secure-portal";
	public static final String CUSTOMER_DB = "customer-db";
	public static final String CUSTOMER_DB_ERROR_PAGE = "customer-db-error-page";
	public static final String PRODUCT_PORTAL = "product-portal";
	public static final String SESSION_PORTAL = "session-portal";
	public static final String INPUT_PORTAL = "input-portal";

	private final String INPUT_PORTAL_URL = APP_SERVER_BASE_URL + "/input-portal";
	private final String CUSTOMER_PORTAL_URL = APP_SERVER_BASE_URL + "/customer-portal";
	private final String PRODUCT_PORTAL_URL = APP_SERVER_BASE_URL + "/product-portal";
	private final String SESSION_PORTAL_URL = APP_SERVER_BASE_URL + "/session-portal";

	@Page
	private AccountSessionsPage accountSessionsPage;

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
	public void testSavedPostRequest() {
		// test login to customer-portal which does a bearer request to customer-db
		driver.get(INPUT_PORTAL_URL);
		assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/input-portal" + slash);
		inputPage.execute("hello");

		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		loginPage.login("bburke@redhat.com", "password");
		assertEquals(driver.getCurrentUrl(), INPUT_PORTAL_URL + "/secured/post");
		String pageSource = driver.getPageSource();
		assertTrue(pageSource.contains("parameter=hello"));

		String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
				.queryParam(OAuth2Constants.REDIRECT_URI, CUSTOMER_PORTAL_URL).build("demo").toString();
		driver.navigate().to(logoutUri);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		driver.get(PRODUCT_PORTAL_URL);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		driver.get(CUSTOMER_PORTAL_URL);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

		// test unsecured POST KEYCLOAK-901
		Client client = ClientBuilder.newClient();
		Form form = new Form();
		form.param("parameter", "hello");
		String text = client.target(INPUT_PORTAL_URL + "/unsecured").request().post(Entity.form(form), String.class);
		assertTrue(text.contains("parameter=hello"));
		client.close();
	}

	@Test
	public void testLoginSSOAndLogout() {
		// test login to customer-portal which does a bearer request to customer-db
		driver.get(CUSTOMER_PORTAL_URL);
		SeleniumUtils.waitGuiForElement(loginPage.getUsernameInput());
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		loginPage.login("bburke@redhat.com", "password");
		assertEquals(driver.getCurrentUrl(), CUSTOMER_PORTAL_URL + slash);
		String pageSource = driver.getPageSource();
		assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

		// test SSO
		driver.get(PRODUCT_PORTAL_URL);
		assertEquals(PRODUCT_PORTAL_URL + slash, driver.getCurrentUrl());
		pageSource = driver.getPageSource();
		assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

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
		assertEquals(1, Integer.parseInt(customerPortalStats.get("active")));
		assertEquals(1, Integer.parseInt(productPortalStats.get("active")));

		// test logout
		String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
				.queryParam(OAuth2Constants.REDIRECT_URI, CUSTOMER_PORTAL_URL).build("demo").toString();
		driver.get(logoutUri);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		driver.get(PRODUCT_PORTAL_URL);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		driver.get(CUSTOMER_PORTAL_URL);
		Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		loginPage.cancel();
		assertTrue(driver.getPageSource().contains("Error Page"));
	}

	@Test
	public void testServletRequestLogout() {
		// test login to customer-portal which does a bearer request to customer-db
		driver.get(CUSTOMER_PORTAL_URL);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		loginPage.login("bburke@redhat.com", "password");
		assertEquals(driver.getCurrentUrl(), CUSTOMER_PORTAL_URL + slash);
		String pageSource = driver.getPageSource();
		assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

		// test SSO
		driver.get(PRODUCT_PORTAL_URL);
		Assert.assertEquals(driver.getCurrentUrl(), PRODUCT_PORTAL_URL + slash);
		pageSource = driver.getPageSource();
		assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

		// back
		driver.get(CUSTOMER_PORTAL_URL);
		assertEquals(driver.getCurrentUrl(), CUSTOMER_PORTAL_URL + slash);
		pageSource = driver.getPageSource();
		Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));
		// test logout

		driver.get(CUSTOMER_PORTAL_URL + "/logout");
		assertTrue(driver.getPageSource().contains("servlet logout ok"));

		driver.get(CUSTOMER_PORTAL_URL);
		String currentUrl = driver.getCurrentUrl();
		assertTrue(currentUrl.startsWith(LOGIN_URL));
		driver.get(PRODUCT_PORTAL_URL);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
	}

	@Test
	public void testLoginSSOIdle() {
		// test login to customer-portal which does a bearer request to customer-db
		driver.get(CUSTOMER_PORTAL_URL);
		SeleniumUtils.waitGuiForElement(loginPage.getUsernameInput());
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		loginPage.login("bburke@redhat.com", "password");
		assertEquals(driver.getCurrentUrl(), CUSTOMER_PORTAL_URL + slash);
		String pageSource = driver.getPageSource();
		assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

		RealmRepresentation demoRealm = keycloak.realm("demo").toRepresentation();
		int originalIdle = demoRealm.getSsoSessionIdleTimeout();
		demoRealm.setSsoSessionIdleTimeout(1);
		keycloak.realm("demo").update(demoRealm);

//		Thread.sleep(2000);
		driver.get(PRODUCT_PORTAL_URL);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

		demoRealm.setSsoSessionIdleTimeout(originalIdle);
		keycloak.realm("demo").update(demoRealm);
	}

	@Test
	@Ignore
	/* Waiting for KEYCLOAK-1478 */
	public void testLoginSSOIdleRemoveExpiredUserSessions() throws Exception {
		// test login to customer-portal which does a bearer request to customer-db
		driver.get(CUSTOMER_PORTAL_URL);
		SeleniumUtils.waitGuiForElement(loginPage.getUsernameInput());
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		loginPage.login("bburke@redhat.com", "password");
		assertEquals(driver.getCurrentUrl(), CUSTOMER_PORTAL_URL + slash);
		String pageSource = driver.getPageSource();
		Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

		RealmRepresentation demoRealm = keycloak.realm("demo").toRepresentation();
		int originalIdle = demoRealm.getSsoSessionIdleTimeout();
		demoRealm.setSsoSessionIdleTimeout(1);
		keycloak.realm("demo").update(demoRealm);

		//TODO finish the test
	}

	@Test
	public void testLoginSSOMax() throws InterruptedException {
		// test login to customer-portal which does a bearer request to customer-db
		driver.get(CUSTOMER_PORTAL_URL);
		SeleniumUtils.waitGuiForElement(loginPage.getUsernameInput());
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		loginPage.login("bburke@redhat.com", "password");
		assertEquals(driver.getCurrentUrl(), CUSTOMER_PORTAL_URL + slash);
		String pageSource = driver.getPageSource();
		Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

		RealmRepresentation demoRealm = keycloak.realm("demo").toRepresentation();
		int originalIdle = demoRealm.getSsoSessionMaxLifespan();
		demoRealm.setSsoSessionMaxLifespan(1);
		keycloak.realm("demo").update(demoRealm);

		TimeUnit.SECONDS.sleep(2);
		driver.get(PRODUCT_PORTAL_URL);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

		demoRealm.setSsoSessionIdleTimeout(originalIdle);
		keycloak.realm("demo").update(demoRealm);
	}

	/**
	 * KEYCLOAK-518
	 */
	@Test
	public void testNullBearerToken() {
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(APP_SERVER_BASE_URL + "/customer-db/");
		Response response = target.request().get();
		assertEquals(401, response.getStatus());
		response.close();
		response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer null").get();
		assertEquals(401, response.getStatus());
		response.close();
		client.close();
	}

	/**
	 * KEYCLOAK-1368
	 */
	@Test
	public void testNullBearerTokenCustomErrorPage() {
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(APP_SERVER_BASE_URL + "/customer-db-error-page/");
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

	/**
	 * KEYCLOAK-518
	 */
	@Test
	public void testBadUser() {
		Client client = ClientBuilder.newClient();
		UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_URL);
		URI uri = OIDCLoginProtocolService.tokenUrl(builder).build("demo");
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
		WebTarget target = client.target(AUTH_SERVER_URL).path("version");
		Version version = target.request().get(Version.class);
		assertNotNull(version);
		assertNotNull(version.getVersion());
		assertNotNull(version.getBuildTime());
		assertNotEquals(version.getVersion(), Version.UNKNOWN);
		assertNotEquals(version.getBuildTime(), Version.UNKNOWN);

		Version version2 = client.target(APP_SERVER_BASE_URL + "/secure-portal").path(AdapterConstants.K_VERSION).request().get(Version.class);
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
		final String SECURE_PORTAL_URL = APP_SERVER_BASE_URL + "/secure-portal";
		driver.get(SECURE_PORTAL_URL);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		loginPage.login("bburke@redhat.com", "password");
		Assert.assertEquals(driver.getCurrentUrl(), SECURE_PORTAL_URL + slash);
		String pageSource = driver.getPageSource();
		assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

		// test logout
		String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
				.queryParam(OAuth2Constants.REDIRECT_URI, SECURE_PORTAL_URL).build("demo").toString();
		driver.get(logoutUri);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		driver.get(SECURE_PORTAL_URL);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
	}

	/**
	 * KEYCLOAK-732
	 *
	 * @param driver2
	 */
	@Test
	public void testSingleSessionInvalidated(@Drone @SecondBrowser WebDriver driver2) {

		loginAndCheckSession(driver, loginPage);

		// cannot pass to loginAndCheckSession becayse loginPage is not working together with driver2, therefore copypasta
		driver2.get(SESSION_PORTAL_URL);
		assertTrue(driver2.getCurrentUrl().startsWith(LOGIN_URL));
		driver2.findElement(By.id("username")).sendKeys("bburke@redhat.com");
		driver2.findElement(By.id("password")).sendKeys("password");
		driver2.findElement(By.id("password")).submit();
		assertEquals(driver2.getCurrentUrl(), SESSION_PORTAL_URL + slash);
		String pageSource = driver2.getPageSource();
		assertTrue(pageSource.contains("Counter=1"));
		// Counter increased now
		driver2.get(SESSION_PORTAL_URL);
		pageSource = driver2.getPageSource();
		assertTrue(pageSource.contains("Counter=2"));

		// Logout in browser1
		String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
				.queryParam(OAuth2Constants.REDIRECT_URI, SESSION_PORTAL_URL).build("demo").toString();
		driver.get(logoutUri);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

		// Assert that I am logged out in browser1
		driver.get(SESSION_PORTAL_URL);
		assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

		// Assert that I am still logged in browser2 and same session is still preserved
		driver2.get(SESSION_PORTAL_URL);
		assertEquals(driver2.getCurrentUrl(), SESSION_PORTAL_URL + slash);
		pageSource = driver2.getPageSource();
		assertTrue(pageSource.contains("Counter=3"));

		driver2.get(logoutUri);
		assertTrue(driver2.getCurrentUrl().startsWith(LOGIN_URL));
	}

	/**
	 * KEYCLOAK-741
	 */
	@Test
	@Ignore
	public void testSessionInvalidatedAfterFailedRefresh() {
		RealmResource demoRealm = keycloak.realm("demo");
		RealmRepresentation demoRealmRep = demoRealm.toRepresentation();
		ClientResource sessionPortal = demoRealm.clients().get("session-portal");
		sessionPortal.toRepresentation().setAdminUrl("");
		int origTokenLifespan = demoRealmRep.getAccessCodeLifespan();
		demoRealmRep.setAccessCodeLifespan(1);
		demoRealm.update(demoRealmRep);

		// Login
		loginAndCheckSession(driver, loginPage);

		// Logout
		String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
				.queryParam(OAuth2Constants.REDIRECT_URI, SESSION_PORTAL_URL).build("demo").toString();
		driver.get(logoutUri);

		// Assert that http session was invalidated
		driver.get(SESSION_PORTAL_URL);
		String currentUrl = driver.getCurrentUrl();
		assertTrue(currentUrl.startsWith(LOGIN_URL));
		loginPage.login("bburke@redhat.com", "password");
		assertEquals(driver.getCurrentUrl(), SESSION_PORTAL_URL + slash);
		String pageSource = driver.getPageSource();
		assertTrue(pageSource.contains("Counter=1"));

		sessionPortal.toRepresentation().setAdminUrl(SESSION_PORTAL_URL);
		demoRealmRep.setAccessCodeLifespan(origTokenLifespan);
		demoRealm.update(demoRealmRep);
	}

	/**
	 * KEYCLOAK-942
	 */
	@Test
	public void testAdminApplicationLogout() {
		// login as bburke
		loginAndCheckSession(driver, loginPage);
		// logout mposolda with admin client
		ApiUtil.findClientByClientId(keycloak.realm("demo"), "session-portal").logoutUser("mposolda");
		// bburke should be still logged with original httpSession in our browser window
		driver.get(SESSION_PORTAL_URL);
		assertEquals(driver.getCurrentUrl(), SESSION_PORTAL_URL + slash);
		String pageSource = driver.getPageSource();
		assertTrue(pageSource.contains("Counter=3"));
		String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
				.queryParam(OAuth2Constants.REDIRECT_URI, SESSION_PORTAL_URL).build("demo").toString();
		driver.get(logoutUri);
	}

	/**
	 * KEYCLOAK-1216
	 */
	@Test
	@Ignore
	public void testAccountManagementSessionsLogout() {
		// login as bburke
		loginAndCheckSession(driver, loginPage);
		driver.get(ADMIN_CONSOLE_URL);
		menuPage.goToAccountManagement();
		accountSessionsPage.sessions();
		accountSessionsPage.logoutAll();
		// Assert I need to login again (logout was propagated to the app)
		loginAndCheckSession(driver, loginPage);
	}

	private void loginAndCheckSession(WebDriver driver, LoginPage loginPage) {
		driver.get(SESSION_PORTAL_URL);
		String currentUrl = driver.getCurrentUrl();
		assertTrue(currentUrl.startsWith(LOGIN_URL));
		loginPage.login("bburke@redhat.com", "password");
		assertEquals(driver.getCurrentUrl(), SESSION_PORTAL_URL + slash);
		String pageSource = driver.getPageSource();
		assertTrue(pageSource.contains("Counter=1"));

		// Counter increased now
		driver.get(SESSION_PORTAL_URL);
		pageSource = driver.getPageSource();
		assertTrue(pageSource.contains("Counter=2"));
	}
}
