package org.keycloak.test.examples;

import org.junit.jupiter.api.Test;
import org.keycloak.test.framework.ui.annotations.InjectPage;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.ui.page.LoginPage;
import org.keycloak.test.framework.ui.page.WelcomePage;

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
