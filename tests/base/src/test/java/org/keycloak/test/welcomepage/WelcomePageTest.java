package org.keycloak.test.welcomepage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.framework.annotations.InjectAdminClient;
import org.keycloak.test.framework.ui.annotations.InjectWebDriver;
import org.keycloak.test.framework.ui.annotations.InjectPage;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.ui.page.WelcomePage;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Collections;
import java.util.Enumeration;
import java.net.URL;
import java.net.InetAddress;
import java.net.NetworkInterface;

@KeycloakIntegrationTest
@TestMethodOrder(OrderAnnotation.class)
public class WelcomePageTest {

    @InjectWebDriver
    WebDriver driver;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectPage
    WelcomePage welcomePage;

    @Test
    @Order(1)
    public void localAccessNoAdmin() throws Exception {
        welcomePage.navigateTo();
        Assertions.assertFalse(welcomePage.isPasswordSet(), "Welcome page did not ask to create a new admin user.");
    }

    @Test
    @Order(2)
    public void remoteAccessNoAdmin() throws Exception {
        driver.get(getPublicServerUrl().toString());
        Assertions.assertFalse(welcomePage.isPasswordSet(), "Welcome page did not ask to create a new admin user.");
    }

    @Test
    @Order(3)
    public void createAdminUser() {
        welcomePage.navigateTo();
        welcomePage.fillRegistration("admin", "admin");
        welcomePage.submit();
        welcomePage.assertUserCreated();

        List<UserRepresentation> users = adminClient.realm("master").users().search("admin", true);
        Assertions.assertEquals(1, users.size());
    }

    @Test
    @Order(4)
    public void localAccessWithAdmin() throws Exception {
        welcomePage.navigateTo();
        Assertions.assertTrue(welcomePage.isPasswordSet(), "Welcome page asked to set admin password.");
    }

    @Test
    @Order(5)
    public void remoteAccessWithAdmin() throws Exception {
        driver.get(getPublicServerUrl().toString());
        Assertions.assertTrue(welcomePage.isPasswordSet(), "Welcome page asked to set admin password.");
    }

    @Test
    @Order(6)
    public void accessCreatedAdminAccount() throws Exception {
        welcomePage.navigateTo();
        welcomePage.navigateToAdminConsole();
        Assertions.assertEquals(driver.getTitle(), "Keycloak Administration Console");
    }

    @Test
    @Order(7)
    public void checkProductNameOnWelcomePage() {
        welcomePage.navigateTo();
//        welcomePage.login("admin", "admin");

        String actualMessage = welcomePage.getWelcomeMessage();
        Assertions.assertEquals("Welcome to Keycloak", actualMessage);
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
}
