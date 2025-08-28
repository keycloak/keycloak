package org.keycloak.test.examples;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.WelcomePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

@KeycloakIntegrationTest
public class PagesTest {

    @InjectAdminClient
    Keycloak adminClient;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    WebDriver webDriver;

    @InjectPage
    WelcomePage welcomePage;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void testLoginFromWelcome() {
        var users = adminClient.realm("master").users();
        users.searchByUsername("admin", true)
                .stream().findFirst().ifPresent(admin ->
                        users.delete(admin.getId()));
        var clients = adminClient.realm("master").clients();
        clients.findByClientId(Config.getAdminClientId())
                .stream().findFirst().ifPresent(client ->
                        clients.delete(client.getId()));

        welcomePage.navigateTo();

        welcomePage.assertCurrent();
        welcomePage.fillRegistration("admin", "admin");
        welcomePage.submit();
        welcomePage.clickOpenAdminConsole();

        if (webDriver instanceof HtmlUnitDriver) {
            String pageId = webDriver.findElement(By.xpath("//body")).getAttribute("data-page-id");
            Assertions.assertEquals("admin", pageId);
            Assertions.assertTrue(webDriver.getCurrentUrl().endsWith("/admin/master/console/"));
        } else {
            loginPage.waitForPage();

            loginPage.assertCurrent();

            loginPage.fillLogin("admin", "admin");
            loginPage.submit();
        }

        assertTrue(runOnServer.fetch(session -> new ApplianceBootstrap(session)
                .createTemporaryMasterRealmAdminService(Config.getAdminClientId(), Config.getAdminClientSecret()), Boolean.class));
        adminClient.tokenManager().refreshToken();
    }

}
