package org.keycloak.test.base;

import org.junit.jupiter.api.Test;
import org.keycloak.test.framework.KeycloakIntegrationTest;
import org.keycloak.test.framework.page.WelcomePage;
import org.keycloak.test.framework.webdriver.TestWebDriver;
import org.openqa.selenium.WebDriver;

@KeycloakIntegrationTest(config = NoAdminUserKeycloakTestServerConfig.class)
public class WelcomePageTest {

    @TestWebDriver
    WebDriver driver;

    @Test
    public void testCreateUser() {
        final var welcomePage = new WelcomePage(driver);
        welcomePage.navigateTo();
        welcomePage.fillRegistration("admin", "admin");
        welcomePage.submit();
        welcomePage.assertUserCreated();
    }

}
