package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

public abstract class AbstractPage {

    protected final ManagedWebDriver driver;

    public AbstractPage(ManagedWebDriver driver) {
        this.driver = driver;
    }

    public abstract String getExpectedPageId();

    public void assertCurrent() {
        driver.waiting().waitForPage(this);
    }
}
