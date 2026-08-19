package org.keycloak.testframework.ui.webdriver;

import java.net.URL;

import org.keycloak.testframework.injection.ManagedTestResource;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public class ManagedWebDriver extends ManagedTestResource {

    private static final ThreadLocal<WebDriver> CURRENT_DRIVER = new ThreadLocal<>();

    private WebDriver driver;

    private AssertionUtils assertionUtils = new AssertionUtils(this);
    private CookieUtils cookieUtils = new CookieUtils(this);
    private PageUtils pageUtils = new PageUtils(this);
    private NavigateUtils  navigateUtils = new NavigateUtils(this);
    private WaitUtils waitUtils = new WaitUtils(this);
    private final BrowserTabUtils tabUtils;

    public ManagedWebDriver(WebDriver driver) {
        this.driver = driver;
        CURRENT_DRIVER.set(driver);
        this.tabUtils = new BrowserTabUtils(this);
    }

    public WebDriver driver() {
        return driver;
    }

    public static WebDriver currentDriver() {
        return CURRENT_DRIVER.get();
    }

    public BrowserType getBrowserType() {
        if (driver instanceof HtmlUnitDriver) {
            return BrowserType.HTML_UNIT;
        } else if (driver instanceof ChromeDriver) {
            return BrowserType.CHROME;
        } else if (driver instanceof FirefoxDriver) {
            return BrowserType.FIREFOX;
        }
        throw new RuntimeException("Unknown browser type: " + driver.getClass());
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public String getPageSource() {
        return driver.getPageSource();
    }

    public WebDriver.Options manage() {
        return driver.manage();
    }

    public WebElement findElement(By by) {
        return driver.findElement(by);
    }

    public java.util.List<WebElement> findElements(By by) {
        return driver.findElements(by);
    }

    public void open(String url) {
        driver.navigate().to(url);
    }

    public void open(URL url) {
        driver.navigate().to(url);
    }

    public AssertionUtils assertions() {
        return assertionUtils;
    }

    public CookieUtils cookies() {
        return cookieUtils;
    }

    public PageUtils page() {
        return pageUtils;
    }

    public NavigateUtils navigate() {
        return navigateUtils;
    }

    public BrowserTabUtils tabs() {
        return tabUtils;
    }

    public WaitUtils waiting() {
        return waitUtils;
    }

    @Override
    public void runCleanup() {
        tabUtils.closeTabs();
    }
}
