package org.keycloak.test.examples;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.WelcomePage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
        welcomePage.navigateTo();

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

    }

}
