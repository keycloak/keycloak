package org.keycloak.testframework.ui.webdriver;

import java.io.File;

import org.keycloak.testframework.config.Config;

import org.htmlunit.WebClientOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverService;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

class DriverUtils {

    static ChromeDriver createChromeDriver(boolean headless) {
        ChromeDriverService.Builder builder = new ChromeDriverService.Builder();

        File driver = resolveDriver("CHROMEWEBDRIVER", "chromedriver");
        if (driver != null) {
            builder.usingDriverExecutable(driver);
        }

        ChromeDriverService driverService = builder.build();
        return new ChromeDriver(driverService, DriverOptions.createChromeOptions(headless));
    }

    static FirefoxDriver  createFirefoxDriver(boolean headless) {
        GeckoDriverService.Builder builder = new GeckoDriverService.Builder();

        File driver = resolveDriver("GECKOWEBDRIVER", "geckodriver");
        if (driver != null) {
            builder.usingDriverExecutable(driver);
        }

        FirefoxDriverService driverService = builder.build();
        return new FirefoxDriver(driverService, DriverOptions.createFirefoxOptions(headless));
    }

    static HtmlUnitDriver createHtmlUnitDriver() {
        HtmlUnitDriver driver = new HtmlUnitDriver(DriverOptions.createHtmlUnitOptions());
        WebClientOptions options = driver.getWebClient().getOptions();
        options.setCssEnabled(false);

        // HtmlUnit doesn't work very well with JS and it's recommended to use this settings.
        // HtmlUnit validates all scripts and then fails. It turned off the validation.
        options.setThrowExceptionOnScriptError(false);
        options.setThrowExceptionOnFailingStatusCode(false);

        return driver;
    }

    private static File resolveDriver(String envName, String driverName) {
        File driver = Config.getValueTypeConfig(ManagedWebDriver.class, "driver", null, File.class);
        if (driver != null) {
            return driver;
        }

        // Environment variable can point to directory where the driver is located, or the driver directly
        String driverPathFromEnv = System.getenv(envName);
        if (driverPathFromEnv != null) {
            driver = new File(driverPathFromEnv);
            if (driver.isFile()) {
                return driver;
            } else {
                return new File(driver, driverName  + (isWindows() ? ".exe" : ""));
            }
        }

        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

}
