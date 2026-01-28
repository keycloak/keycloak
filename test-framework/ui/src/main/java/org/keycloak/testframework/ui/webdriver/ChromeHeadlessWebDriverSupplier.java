package org.keycloak.testframework.ui.webdriver;

import org.openqa.selenium.WebDriver;

public class ChromeHeadlessWebDriverSupplier extends AbstractWebDriverSupplier {

    @Override
    public String getAlias() {
        return "chrome-headless";
    }

    @Override
    public WebDriver getWebDriver() {
        return DriverUtils.createChromeDriver(true);
    }
}
