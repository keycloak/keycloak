package org.keycloak.testframework.ui.webdriver;

import org.openqa.selenium.WebDriver;

public class HtmlUnitWebDriverSupplier extends AbstractWebDriverSupplier {

    @Override
    public String getAlias() {
        return "htmlunit";
    }

    @Override
    public WebDriver getWebDriver() {
        return DriverUtils.createHtmlUnitDriver();
    }
}
