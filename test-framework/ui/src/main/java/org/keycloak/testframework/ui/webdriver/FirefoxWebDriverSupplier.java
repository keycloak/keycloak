package org.keycloak.testframework.ui.webdriver;

import org.openqa.selenium.WebDriver;

public class FirefoxWebDriverSupplier extends AbstractWebDriverSupplier {

    @Override
    public String getAlias() {
        return "firefox";
    }

    @Override
    public WebDriver getWebDriver() {
        return DriverUtils.createFirefoxDriver(false);
    }
}
