package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.WelcomePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

@KeycloakIntegrationTest
public class PagesTest {

    @InjectWebDriver
    WebDriver webDriver;

    @InjectPage
    WelcomePage welcomePage;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void testLoginFromWelcome() {
        welcomePage.navigateTo();

        if (welcomePage.isActivePage()) {
            welcomePage.fillRegistration("admin", "admin");
            welcomePage.submit();
            welcomePage.clickOpenAdminConsole();
        }

        if (webDriver instanceof HtmlUnitDriver) {
            String pageId = webDriver.findElement(By.xpath("//body")).getAttribute("data-page-id");
            Assertions.assertEquals("admin", pageId);
            Assertions.assertTrue(webDriver.getCurrentUrl().endsWith("/admin/master/console/"));
        } else {
            loginPage.waitForPage();

            Assertions.assertTrue(loginPage.isActivePage());

            loginPage.fillLogin("admin", "admin");
            loginPage.submit();
        }
    }

}
