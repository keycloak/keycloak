package org.keycloak.tests.welcomepage;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.AdminPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.WelcomePage;
import org.keycloak.testframework.ui.webdriver.BrowserType;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = WelcomePageTest.WelcomePageTestConfig.class)
@TestMethodOrder(OrderAnnotation.class)
public class WelcomePageTest {

    // force the creation of a new server
    static class WelcomePageTestConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("http-host", "0.0.0.0");
        }
    }

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectPage
    AdminPage adminPage;

    @InjectPage
    WelcomePage welcomePage;

    @InjectPage
    LoginPage loginPage;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    @Order(1)
    public void localAccessNoAdminNorServiceAccount() {
        // get rid of the bootstrap admin user and client
        // it will get added back in subsequent tests
        var users = adminClient.realms().realm("master").users();
        users.searchByUsername(Config.getAdminUsername(), true).stream().findFirst().ifPresent(admin -> users.delete(admin.getId()));
        var clients = adminClient.realms().realm("master").clients();
        clients.findByClientId(Config.getAdminClientId()).stream().findFirst().ifPresent(client -> clients.delete(client.getId()));

        driver.open(keycloakUrls.getBaseUrl());

        Assertions.assertEquals("Create an administrative user", welcomePage.getWelcomeMessage());
        Assertions.assertTrue(welcomePage.getWelcomeDescription().startsWith("To get started with Keycloak, you first create an administrative user"));
        Assertions.assertTrue(driver.page().getPageSource().contains("form"));
    }

    @Test
    @Order(2)
    public void remoteAccessNoAdmin() throws Exception {
        driver.open(getPublicServerUrl());

        Assertions.assertEquals("Local access required", welcomePage.getWelcomeMessage());
        Assertions.assertTrue(welcomePage.getWelcomeDescription().startsWith("You will need local access to create the administrative user."));
        Assertions.assertFalse(driver.page().getPageSource().contains("form"));
    }

    @Test
    @Order(3)
    public void createAdminUser() {
        driver.open(keycloakUrls.getBaseUrl());
        welcomePage.fillRegistration(Config.getAdminUsername(), "Sebastian", "BestAdminInTheWorld", "admin@localhost", Config.getAdminPassword());
        welcomePage.submit();

        Assertions.assertTrue(welcomePage.getPageAlert().contains("User created"));

        // re-establish the service account so that the admin client will work
        assertTrue(runOnServer.fetch(session -> new ApplianceBootstrap(session)
                .createTemporaryMasterRealmAdminService(Config.getAdminClientId(), Config.getAdminClientSecret()), Boolean.class));

        adminClient.tokenManager().refreshToken();

        List<UserRepresentation> users = adminClient.realm("master").users().search(Config.getAdminUsername(), true);
        Assertions.assertEquals(1, users.size());

        UserRepresentation adminUser = users.get(0);
        Assertions.assertEquals("Sebastian", adminUser.getFirstName());
        Assertions.assertEquals("BestAdminInTheWorld", adminUser.getLastName());
        Assertions.assertEquals("admin@localhost", adminUser.getEmail());
    }

    @Test
    @Order(4)
    public void localAccessWithAdmin() {
        driver.open(keycloakUrls.getBaseUrl());
        adminPage.assertCurrent();
    }

    @Test
    @Order(5)
    public void remoteAccessWithAdmin() throws Exception {
        driver.open(getPublicServerUrl().toString());
        adminPage.assertCurrent();
    }

    @Test
    @Order(6)
    public void accessCreatedAdminAccount() throws MalformedURLException {
        driver.open(keycloakUrls.getBaseUrl());

        // HtmlUnit does not support Admin Console as it uses JavaScript modules, so faking the redirect to login pages
        if (driver.getBrowserType().equals(BrowserType.HTML_UNIT)) {
            driver.open(getFakeLoginRedirect());
        }

        loginPage.assertCurrent();
        loginPage.fillLogin(Config.getAdminUsername(), Config.getAdminPassword());
        loginPage.submit();

        adminPage.assertCurrent();
    }

    /**
     * Attempt to resolve the floating IP address. This is where Quarkus
     * will be accessible.
     *
     * @return
     * @throws Exception
     */
    private static String getFloatingIpAddress() throws Exception {
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

    static URL getPublicServerUrl() throws Exception {
        String floatingIp = getFloatingIpAddress();
        if (floatingIp == null) {
            throw new RuntimeException("Could not determine floating IP address.");
        }
        return new URL("http", floatingIp, 8080, "");
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
