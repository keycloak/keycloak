package org.keycloak.testframework.ui.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

public class HtmlUnitWebDriverSupplier extends AbstractWebDriverSupplier {

    @Override
    public String getAlias() {
        return "htmlunit";
    }

    @Override
    public WebDriver getWebDriver() {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        setCommonCapabilities(capabilities);

        capabilities.setBrowserName("htmlunit");
        capabilities.setCapability(HtmlUnitDriver.DOWNLOAD_IMAGES_CAPABILITY, false);
        capabilities.setCapability(HtmlUnitDriver.JAVASCRIPT_ENABLED, true);

        HtmlUnitDriver driver = new HtmlUnitDriver(capabilities);
        driver.getWebClient().getOptions().setCssEnabled(false);
        return driver;
    }
}
