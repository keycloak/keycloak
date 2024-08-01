package org.keycloak.test.framework.flow;

import org.jboss.logging.Logger;
import org.keycloak.test.framework.page.LoginPage;
import org.openqa.selenium.WebDriver;

public class ManagedFlowLibrary {

    private static final Logger LOGGER = Logger.getLogger(ManagedFlowLibrary.class);

    public ManagedFlowLibrary() {
    }

    public ManagedFlowLibrary navigateToAdmin(WebDriver driver) {
        driver.get("http://localhost:8080");
        return this;
    }

    public ManagedFlowLibrary loginToAdmin(LoginPage page) {
        page.fillLogin("admin", "admin");
        page.submit();
        return this;
    }

    public ManagedFlowLibrary logout() {

        return this;
    }

    public void complete(String message) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.tracev("Executing managed flow: {0}",
                    message);
        }
    }
}
