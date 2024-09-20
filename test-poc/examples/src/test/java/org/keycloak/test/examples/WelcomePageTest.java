package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.framework.annotations.InjectAdminClient;
import org.keycloak.test.framework.annotations.InjectWebDriver;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.page.WelcomePage;
import org.openqa.selenium.WebDriver;

import java.util.List;

@KeycloakIntegrationTest
public class WelcomePageTest {

    @InjectWebDriver
    WebDriver driver;

    @InjectAdminClient
    Keycloak adminClient;

    @Test
    public void testCreateUser() {
        final var welcomePage = new WelcomePage(driver);
        welcomePage.navigateTo();
        welcomePage.fillRegistration("admin", "admin");
        welcomePage.submit();
        welcomePage.assertUserCreated();

        List<UserRepresentation> users = adminClient.realm("master").users().search("admin", true);
        Assertions.assertEquals(1, users.size());

        adminClient.realm("master").users().get(users.get(0).getId()).remove();
    }

}
