package org.keycloak.test.framework.flow;

import org.jboss.logging.Logger;
import org.keycloak.test.framework.page.LoginPage;
import org.keycloak.test.framework.page.WelcomePage;
import org.openqa.selenium.WebDriver;

public class LoginFlowLibrary {

    private WelcomePage welcomePage;
    private LoginPage loginPage;

    private static final Logger LOGGER = Logger.getLogger(LoginFlowLibrary.class);

    public LoginFlowLibrary(WebDriver driver) {
        this.welcomePage = new WelcomePage(driver);
        this.loginPage = new LoginPage(driver);
    }

    public LoginFlowLibrary navigateToWelcomeScreen() {
        welcomePage.navigateTo();

        return this;
    }

    public LoginFlowLibrary loginAsAdmin() {
        loginPage.fillLogin("admin", "admin");
        loginPage.submit();

        return this;
    }

    public LoginFlowLibrary logout() {

        return this;
    }

    public LoginFlowLibrary execute(String message) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.tracev("Executing managed flow: {0}",
                    message);
        }

        return this;
    }

    public void complete(String message) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.tracev("Managed flow completed: {0}",
                    message);
        }
    }
}
