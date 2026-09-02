package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

public class AdminPage extends AbstractPage {

    public AdminPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "admin";
    }

}
