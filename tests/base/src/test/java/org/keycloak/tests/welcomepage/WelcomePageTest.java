package org.keycloak.tests.welcomepage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.WelcomePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@KeycloakIntegrationTest
@TestMethodOrder(OrderAnnotation.class)
public class WelcomePageTest {

    @InjectWebDriver
    WebDriver driver;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectPage
    WelcomePage welcomePage;

    @InjectPage
    LoginPage loginPage;

    @Test
    @Order(1)
    public void localAccessNoAdmin() {
        welcomePage.navigateTo();

        Assertions.assertEquals("Create a temporary administrative user", welcomePage.getWelcomeMessage());
        Assertions.assertTrue(welcomePage.getWelcomeDescription().startsWith("To get started with Keycloak, you first create a temporary administrative user"));
        Assertions.assertTrue(driver.getPageSource().contains("form"));
    }

    @Test
    @Order(2)
    public void remoteAccessNoAdmin() throws Exception {
        driver.get(getPublicServerUrl().toString());

        Assertions.assertEquals("Local access required", welcomePage.getWelcomeMessage());
        Assertions.assertTrue(welcomePage.getWelcomeDescription().startsWith("You will need local access to create the temporary administrative user."));
        Assertions.assertFalse(driver.getPageSource().contains("form"));
    }

    @Test
    @Order(3)
    public void createAdminUser() {
        welcomePage.navigateTo();
        welcomePage.fillRegistration("admin", "admin");
        welcomePage.submit();

        Assertions.assertTrue(welcomePage.getPageAlert().contains("User created"));

        List<UserRepresentation> users = adminClient.realm("master").users().search("admin", true);
        Assertions.assertEquals(1, users.size());
    }

    @Test
    @Order(4)
    public void localAccessWithAdmin() {
        welcomePage.navigateTo();

        assertOnAdminConsole();
    }

    @Test
    @Order(5)
    public void remoteAccessWithAdmin() throws Exception {
        driver.get(getPublicServerUrl().toString());

        assertOnAdminConsole();
    }

    @Test
    @Order(6)
    public void accessCreatedAdminAccount() throws MalformedURLException {
        welcomePage.navigateTo();

        // HtmlUnit does not support Admin Console as it uses JavaScript modules, so faking the redirect to login pages
        if (driver.getClass().equals(HtmlUnitDriver.class)) {
            driver.navigate().to(getFakeLoginRedirect());
        }

        loginPage.fillLogin("admin", "admin");
        loginPage.submit();

        Assertions.assertEquals(driver.getTitle(), "Keycloak Administration Console");
    }

    /**
     * Attempt to resolve the floating IP address. This is where Quarkus
     * will be accessible.
     *
     * @return
     * @throws Exception
     */
    private String getFloatingIpAddress() throws Exception {
        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface ni : Collections.list(netInterfaces)) {
            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            for (InetAddress a : Collections.list(inetAddresses)) {
                if (!a.isLoopbackAddress() && a.isSiteLocalAddress()) {
                    return a.getHostAddress();
                }
            }
        }
        return null;
    }

    private URL getPublicServerUrl() throws Exception {
        String floatingIp = getFloatingIpAddress();
        if (floatingIp == null) {
            throw new RuntimeException("Could not determine floating IP address.");
        }
        return new URL("http", floatingIp, 8080, "");
    }

    private void assertOnAdminConsole() {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> driver.getTitle().equals("Keycloak Administration Console") || driver.getTitle().equals("Sign in to Keycloak"));
    }

    private URL getFakeLoginRedirect() throws MalformedURLException {
        KeycloakUriBuilder uriBuilder = KeycloakUriBuilder.fromUri("http://localhost:8080/realms/master/protocol/openid-connect/auth");
        uriBuilder.queryParam("client_id", "security-admin-console");
        uriBuilder.queryParam("redirect_uri", "http://localhost:8080/admin/master/console/");
        uriBuilder.queryParam("state", "randomstate");
        uriBuilder.queryParam("response_mode", "query");
        uriBuilder.queryParam("response_type", "code");
        uriBuilder.queryParam("scope", "openid");
        uriBuilder.queryParam("nonce", "randomnonce");
        uriBuilder.queryParam("code_challenge", "UV90ZNinyGsxyNlz6A08FQzDXbA7NCjkrCZv7PgeVxA");
        uriBuilder.queryParam("code_challenge_method", "S256");
        return uriBuilder.build().toURL();
    }

}
