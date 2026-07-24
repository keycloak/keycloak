package org.keycloak.testframework.ui.webdriver;

import org.openqa.selenium.WebDriver;

public class FirefoxHeadlessWebDriverSupplier extends AbstractWebDriverSupplier {

    @Override
    public String getAlias() {
        return "firefox-headless";
    }

    @Override
    public WebDriver getWebDriver() {
        return DriverUtils.createFirefoxDriver(true);
    }
}
