package org.keycloak.testframework.ui.webdriver;

import java.time.Duration;
import java.util.Map;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

class DriverOptions {

    static ChromeOptions createChromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        setCommonCapabilities(options);

        if (headless) {
            options.addArguments(
                    "--headless=new",
                    "--disable-gpu",
                    "--window-size=1920,1200",
                    "--ignore-certificate-errors",
                    "--disable-dev-shm-usage",
                    "--remote-allow-origins=*",
                    "--no-sandbox"
            );
        }

        return options;
    }

    static FirefoxOptions createFirefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        setCommonCapabilities(options);

        if (headless) {
            options.addArguments("-headless");
        }

        options.addPreference("extensions.update.enabled", "false");
        options.addPreference("app.update.enabled", "false");
        options.addPreference("app.update.auto", "false");

        return options;
    }

    static DesiredCapabilities createHtmlUnitOptions() {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        setCommonCapabilities(capabilities);

        capabilities.setBrowserName("htmlunit");
        capabilities.setCapability(HtmlUnitDriver.DOWNLOAD_IMAGES_CAPABILITY, false);
        capabilities.setCapability(HtmlUnitDriver.JAVASCRIPT_ENABLED, true);

        return capabilities;
    }

    private static void setCommonCapabilities(MutableCapabilities capabilities) {
        capabilities.setCapability(CapabilityType.PAGE_LOAD_STRATEGY, PageLoadStrategy.NORMAL.toString());
        capabilities.setCapability("timeouts", Map.of("implicit", Duration.ofSeconds(5).toMillis()));
    }

}
