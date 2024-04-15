package org.keycloak.testsuite.webdriver;

import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.time.Duration;

public class WebDriverLifecycle {
    private final String browser;

    private final String chromeArguments;

    private final String firefoxArguments;

    private final String firefoxUserPreferences;

    private final String browserVersion;

    public WebDriverLifecycle() {
        this.browser = System.getProperty("browser");
        this.chromeArguments = System.getProperty("chromeArguments");
        this.firefoxArguments = System.getProperty("firefoxArguments");
        this.firefoxUserPreferences = System.getProperty("firefoxUserPreferences");
        this.browserVersion = System.getProperty("browserVersion");
    }

    public WebDriver startBrowser() {
        switch (this.browser) {
            case "chrome":
                return startChromeBrowser();
            case "firefox":
                return startFirefoxBrowser();
            case "htmlunit":
                return startHtmlUnitDriver();
            default:
                return startDefaultBrowser();
        }
    }

    public WebDriver startDefaultBrowser() {
        return startChromeBrowser();
    }

    public WebDriver startChromeBrowser () {
        ChromeOptions options = new ChromeOptions();
        options.setImplicitWaitTimeout(Duration.ofMillis(1000));
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.addArguments(this.chromeArguments);
        if(!this.browserVersion.isEmpty()) {
            options.setCapability("browserVersion", this.browserVersion);
        }

        return new ChromeDriver(options);
    }

    public WebDriver startFirefoxBrowser () {
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("profile", firefoxUserPreferences);

        FirefoxOptions options = new FirefoxOptions();
        options.setImplicitWaitTimeout(Duration.ofMillis(1000));
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.addArguments(this.firefoxArguments);
        if(!this.browserVersion.isEmpty()) {
            options.setCapability("browserVersion", this.browserVersion);
        }
        if(!this.firefoxUserPreferences.isEmpty()) {
            options.setProfile(profile);
        }

        return new FirefoxDriver(options);
    }

    public WebDriver startHtmlUnitDriver () {
        return new HtmlUnitDriver(BrowserVersion.CHROME, false) {
            @Override
            protected WebClient modifyWebClient(WebClient client) {
                final WebClient webClient = super.modifyWebClient(client);
                webClient.getOptions().setCssEnabled(false);
                webClient.getOptions().setHistoryPageCacheLimit(1);

                return webClient;
            }
        };
    }

    public void stopBrowser(WebDriver webDriver) {
        webDriver.quit();
    }
}
