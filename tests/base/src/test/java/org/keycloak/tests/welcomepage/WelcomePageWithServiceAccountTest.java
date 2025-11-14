package org.keycloak.tests.welcomepage;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.WelcomePage;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriver;

import static org.keycloak.tests.welcomepage.WelcomePageTest.assertOnAdminConsole;
import static org.keycloak.tests.welcomepage.WelcomePageTest.getPublicServerUrl;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@KeycloakIntegrationTest(config = WelcomePageWithServiceAccountTest.WelcomePageWithServiceAccountTestConfig.class)
@TestMethodOrder(OrderAnnotation.class)
public class WelcomePageWithServiceAccountTest {

    // force the creation of a new server
    static class WelcomePageWithServiceAccountTestConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config;
        }
    }

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    WebDriver driver;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectPage
    WelcomePage welcomePage;

    @Test
    @Order(1)
    public void localAccessWithServiceAccount() {
        // get rid of the admin user - the service account should still exist
        RealmResource masterRealm = adminClient.realms().realm("master");
        UsersResource users = masterRealm.users();
        masterRealm.users().searchByUsername(Config.getAdminUsername(), true).stream().findFirst().ifPresent(admin -> users.delete(admin.getId()));

        welcomePage.navigateTo();

        assertOnAdminConsole(driver);
    }

    @Test
    @Order(2)
    public void remoteAccessWithServiceAccount() throws Exception {
        driver.get(getPublicServerUrl().toString());

        assertOnAdminConsole(driver);
    }

    @Test
    @Order(3)
    public void createAdminUser() throws Exception {
        // should fail because the service account user already exists
        assertFalse(runOnServer.fetch(session -> new ApplianceBootstrap(session)
                .createMasterRealmAdminUser(Config.getAdminUsername(), Config.getAdminPassword(), true, true), Boolean.class));

        // should succeed as a non-initial user
        assertTrue(runOnServer.fetch(session -> new ApplianceBootstrap(session)
                .createMasterRealmAdminUser(Config.getAdminUsername(), Config.getAdminPassword(), true, false), Boolean.class));
    }

}
