package org.keycloak.test.base;

import org.junit.jupiter.api.Test;
import org.keycloak.test.framework.KeycloakIntegrationTest;
import org.keycloak.test.framework.page.LoginPage;
import org.keycloak.test.framework.page.TestPage;
import org.keycloak.test.framework.page.WelcomePage;

@KeycloakIntegrationTest
public class PagesTest {

    @TestPage
    WelcomePage welcomePage;

    @TestPage
    LoginPage loginPage;

    @Test
    public void testLoginFromWelcome() {
        welcomePage.navigateTo();
        loginPage.fillLogin("admin", "admin");
        loginPage.submit();
    }

}
