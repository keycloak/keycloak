package org.keycloak.testframework.ui.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class ChromeWebDriverSupplier extends AbstractWebDriverSupplier {

    @Override
    public String getAlias() {
        return "chrome";
    }

    @Override
    public WebDriver getWebDriver() {
        ChromeOptions options = new ChromeOptions();
        setCommonCapabilities(options);

        String chromeBinary = resolveDriverBinary("CHROMEWEBDRIVER", "ChromeWebDriver");
        if (chromeBinary != null) {
            options.setBinary(chromeBinary);
        }

        return new ChromeDriver(options);
    }
}
