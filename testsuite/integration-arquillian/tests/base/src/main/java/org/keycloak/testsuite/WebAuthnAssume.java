package org.keycloak.testsuite;

import org.junit.Assume;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.RemoteWebDriver;

import static org.keycloak.testsuite.util.DroneUtils.getCurrentDriver;

public class WebAuthnAssume {

    public static final String CHROME_NAME = BrowserType.CHROME;
    public static final int CHROME_MIN_VERSION = 68;

    public static void assumeChrome() {
        assumeChrome(getCurrentDriver());
    }

    public static void assumeChrome(WebDriver driver) {
        Assume.assumeNotNull(driver);
        String chromeArguments = System.getProperty("chromeArguments");
        Assume.assumeNotNull(chromeArguments);
        Assume.assumeTrue(chromeArguments.contains("--enable-web-authentication-testing-api"));
        Assume.assumeTrue("Browser must be Chrome (RemoteWebDriver)!", driver instanceof RemoteWebDriver);
        Capabilities cap = ((RemoteWebDriver) driver).getCapabilities();
        String browserName = cap.getBrowserName().toLowerCase();
        int version = Integer.parseInt(cap.getVersion().substring(0, cap.getVersion().indexOf(".")));

        Assume.assumeTrue("Browser must be Chrome !", browserName.equals(CHROME_NAME));
        Assume.assumeTrue("Version of chrome must be higher than or equal to " + CHROME_MIN_VERSION, version >= CHROME_MIN_VERSION);
    }
}
