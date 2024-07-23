package org.keycloak.test.examples;

import org.junit.jupiter.api.Test;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.page.LoginPage;
import org.keycloak.test.framework.annotations.Page;
import org.keycloak.test.framework.page.WelcomePage;

@KeycloakIntegrationTest
public class PagesTest {

    @Page
    WelcomePage welcomePage;

    @Page
    LoginPage loginPage;

    @Test
    public void testLoginFromWelcome() {
        welcomePage.navigateTo();
        loginPage.fillLogin("admin", "admin");
        loginPage.submit();
    }

}
