package org.keycloak.testsuite.util;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class BrowserDriverUtil {

    public static boolean isDriverInstanceOf(WebDriver driver, Class<? extends WebDriver> clazz) {
        return clazz.isAssignableFrom(driver.getClass());
    }

    public static boolean isDriverChrome(WebDriver driver) {
        return isDriverInstanceOf(driver, ChromeDriver.class);
    }

    public static boolean isDriverFirefox(WebDriver driver) {
        return isDriverInstanceOf(driver, FirefoxDriver.class);
    }
}
