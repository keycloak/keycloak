package org.keycloak.test.base;

import org.junit.jupiter.api.Test;
import org.keycloak.test.framework.KeycloakIntegrationTest;
import org.keycloak.test.framework.TestPage;
import org.keycloak.test.framework.webdriver.KeycloakWebDriver;
import org.keycloak.test.framework.page.WelcomePage;
import org.keycloak.test.framework.server.NoAdminUserKeycloakTestServerConfig;
import org.openqa.selenium.WebDriver;

@KeycloakIntegrationTest(config = NoAdminUserKeycloakTestServerConfig.class)
public class WelcomePageTest {

    @TestPage
    WelcomePage welcomePage;

    @Test
    public void testCreateUser() {
        welcomePage.navigateTo();
        welcomePage.fillRegistration("admin", "admin");
        welcomePage.submit();
        welcomePage.assertUserCreated();
    }

}
