package org.keycloak.test.framework.webdriver;

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
        setGlobalOptions(options);
        options.addArguments(
                "--headless",
                "--disable-gpu",
                "--window-size=1920,1200",
                "--ignore-certificate-errors",
                "--disable-dev-shm-usage"
        );
        return new ChromeDriver(options);
    }
}
