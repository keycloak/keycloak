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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        loginPage.fillLogin("admin", "admin");
        loginPage.submit();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
