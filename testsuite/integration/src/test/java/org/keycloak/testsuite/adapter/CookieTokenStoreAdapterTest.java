package org.keycloak.testsuite.adapter;

import java.net.URL;

import javax.ws.rs.core.UriBuilder;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.testutils.KeycloakServer;
import org.keycloak.util.Time;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

/**
 * KEYCLOAK-702
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CookieTokenStoreAdapterTest {

    public static final String LOGIN_URL = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri("http://localhost:8081/auth")).build("demo").toString();

    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            // Other tests may left Time offset uncleared, which could cause issues
            Time.setOffset(0);

            RealmRepresentation representation = KeycloakServer.loadJson(getClass().getResourceAsStream("/adapter-test/demorealm.json"), RealmRepresentation.class);
            manager.importRealm(representation);

            URL url = getClass().getResource("/adapter-test/cust-app-keycloak.json");
            deployApplication("customer-portal", "/customer-portal", CustomerServlet.class, url.getPath(), "user");
            url = getClass().getResource("/adapter-test/cust-app-cookie-keycloak.json");
            deployApplication("customer-cookie-portal", "/customer-cookie-portal", CustomerServlet.class, url.getPath(), "user");
            url = getClass().getResource("/adapter-test/customer-db-keycloak.json");
            deployApplication("customer-db", "/customer-db", CustomerDatabaseServlet.class, url.getPath(), "user");
        }
    };

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @Test
    public void testTokenInCookieSSO() throws Throwable {
        // Login
        String tokenCookie = loginToCustomerCookiePortal();

        // SSO to second app
        driver.navigate().to("http://localhost:8081/customer-portal");
        assertLogged();

        // return to customer-cookie-portal and assert still same cookie (accessToken didn't expire)
        driver.navigate().to("http://localhost:8081/customer-cookie-portal");
        assertLogged();
        String tokenCookie2 = driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE).getValue();
        Assert.assertEquals(tokenCookie, tokenCookie2);

        // Logout with httpServletRequest
        logoutFromCustomerCookiePortal();

        // Also should be logged-out from the second app
        driver.navigate().to("http://localhost:8081/customer-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
    }

    @Test
    public void testTokenInCookieRefresh() throws Throwable {
        try {
            // Set token timeout 1 sec
            KeycloakSession session = keycloakRule.startSession();
            RealmModel realm = session.realms().getRealmByName("demo");
            int originalTokenTimeout = realm.getAccessTokenLifespan();
            realm.setAccessTokenLifespan(3);
            session.getTransaction().commit();
            session.close();

            // login to customer-cookie-portal
            String tokenCookie1 = loginToCustomerCookiePortal();

            // Simulate waiting 4 seconds (Running testsuite in real env like Wildfly or EAP may still require to do Thread.sleep to really wait 4 seconds...)
            Time.setOffset(4);
            //Thread.sleep(4000);

            // assert cookie was refreshed
            driver.navigate().to("http://localhost:8081/customer-cookie-portal");
            Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/customer-cookie-portal");
            assertLogged();
            String tokenCookie2 = driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE).getValue();
            Assert.assertNotEquals(tokenCookie1, tokenCookie2);

            // login to 2nd app and logout from it
            driver.navigate().to("http://localhost:8081/customer-portal");
            Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/customer-portal");
            assertLogged();

            driver.navigate().to("http://localhost:8081/customer-portal/logout");
            Assert.assertTrue(driver.getPageSource().contains("servlet logout ok"));
            driver.navigate().to("http://localhost:8081/customer-portal");
            Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

            // Simulate another 4 seconds
            Time.setOffset(8);

            // assert not logged in customer-cookie-portal
            driver.navigate().to("http://localhost:8081/customer-cookie-portal");
            Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

            // Change timeout back
            Time.setOffset(0);
            session = keycloakRule.startSession();
            realm = session.realms().getRealmByName("demo");
            realm.setAccessTokenLifespan(originalTokenTimeout);
            session.getTransaction().commit();
            session.close();
        } finally {
            Time.setOffset(0);
        }
    }

    @Test
    public void testInvalidTokenCookie() throws Throwable {
        // Login
        String tokenCookie = loginToCustomerCookiePortal();
        String changedTokenCookie = tokenCookie.replace("a", "b");

        // change cookie to invalid value
        driver.manage().addCookie(new Cookie(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE, changedTokenCookie, "/customer-cookie-portal"));

        // visit page and assert re-logged and cookie was refreshed
        driver.navigate().to("http://localhost:8081/customer-cookie-portal");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/customer-cookie-portal");
        String currentCookie = driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE).getValue();
        Assert.assertNotEquals(currentCookie, tokenCookie);
        Assert.assertNotEquals(currentCookie, changedTokenCookie);

        // logout
        logoutFromCustomerCookiePortal();
    }

    // login to customer-cookie-portal and return the KEYCLOAK_ADAPTER_STATE cookie established on adapter
    private String loginToCustomerCookiePortal() {
        driver.navigate().to("http://localhost:8081/customer-cookie-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/customer-cookie-portal");
        assertLogged();

        // Assert no JSESSIONID cookie
        Assert.assertNull(driver.manage().getCookieNamed("JSESSIONID"));

        return driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE).getValue();
    }

    private void logoutFromCustomerCookiePortal() {
        driver.navigate().to("http://localhost:8081/customer-cookie-portal/logout");
        Assert.assertTrue(driver.getPageSource().contains("servlet logout ok"));
        Assert.assertNull(driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE));
        driver.navigate().to("http://localhost:8081/customer-cookie-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
    }

    private void assertLogged() {
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));
    }
}
