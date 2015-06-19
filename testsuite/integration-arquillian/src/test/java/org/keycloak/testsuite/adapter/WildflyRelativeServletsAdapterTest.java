package org.keycloak.testsuite.adapter;

import java.util.List;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.CUSTOMER_PORTAL;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.PRODUCT_PORTAL;
import org.keycloak.testsuite.arquillian.AuthServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AuthServerContainer("auth-server-wildfly")
@Ignore("doesn't work yet")
public class WildflyRelativeServletsAdapterTest extends AbstractServletsAdapterTest {

	private static boolean servletsDeployed = false;

	public WildflyRelativeServletsAdapterTest() {
		super(AUTH_SERVER_BASE_URL);
	}

	@Before
	public void deployServlets() {
		if (!servletsDeployed) {
			importRealm("/adapter-test/demorealm-relative.json");
			deployer.deploy(CUSTOMER_PORTAL);
//            deployer.deploy(SECURE_PORTAL);
//            deployer.deploy(CUSTOMER_DB);
//            deployer.deploy(CUSTOMER_DB_ERROR_PAGE);
			deployer.deploy(PRODUCT_PORTAL);
//            deployer.deploy(SESSION_PORTAL);
//            deployer.deploy(INPUT_PORTAL);
			servletsDeployed = true;
		}
	}

	@Test
	@Override
	public void testLoginSSOAndLogout() {
		// test login to customer-portal which does a bearer request to customer-db
		driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
		Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		loginPage.login("bburke@redhat.com", "password");
		Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/customer-portal");
		String pageSource = driver.getPageSource();
		Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

		// test SSO
		driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
		Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/product-portal");
		pageSource = driver.getPageSource();
		Assert.assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

		// View stats
		List<Map<String, String>> stats = Keycloak.getInstance(APP_SERVER_BASE_URL + "/auth", "master", "admin", "admin", "security-admin-console").realm("demo").getClientSessionStats();
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
		String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(APP_SERVER_BASE_URL + "/auth"))
				.queryParam(OAuth2Constants.REDIRECT_URI, "/customer-portal").build("demo").toString();
		driver.navigate().to(logoutUri);
		Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
		Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
		Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
	}

	@Test
	@Override
	public void testServletRequestLogout() {
		driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
		Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
		loginPage.login("bburke@redhat.com", "password");
		Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/customer-portal");
		Assert.assertTrue(driver.getPageSource().contains("Bill Burke"));

		driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
		Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/product-portal");
		Assert.assertTrue(driver.getPageSource().contains("iPhone"));

		// test logout
		driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal/logout");
		Assert.assertTrue(driver.getPageSource().contains("servlet logout ok"));

		driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
		String currentUrl = driver.getCurrentUrl();
		Assert.assertTrue(currentUrl.startsWith(LOGIN_URL));
		driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
		Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
	}

	@Override
	@Test
	@Ignore
	public void testSavedPostRequest() {
	}

}
