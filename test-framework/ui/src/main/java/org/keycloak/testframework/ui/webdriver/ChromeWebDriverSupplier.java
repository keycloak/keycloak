package org.keycloak.testframework.ui.webdriver;

import org.keycloak.testframework.ui.annotations.InjectWebDriver;

import org.openqa.selenium.WebDriver;

public class ChromeWebDriverSupplier extends AbstractWebDriverSupplier {

    @Override
    public String getAlias() {
        return "chrome";
    }

    @Override
    public WebDriver getWebDriver() {
        return DriverUtils.createChromeDriver(false);
    }

    @Override
    public WebDriver getWebDriver(InjectWebDriver annotation) {
        return DriverUtils.createChromeDriver(false, annotation.chromeArguments());
    }
}
