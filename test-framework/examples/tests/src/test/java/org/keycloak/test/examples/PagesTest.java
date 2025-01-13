package org.keycloak.test.examples;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.WelcomePage;

@KeycloakIntegrationTest
public class PagesTest {

    @InjectPage
    WelcomePage welcomePage;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void testLoginFromWelcome() {
        welcomePage.navigateTo();
        loginPage.fillLogin("admin", "admin");
        loginPage.submit();
    }

}
