package org.keycloak.testframework.tests;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.BrowserType;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class WebDriverChromeArgumentsTest {

    @InjectWebDriver(ref = "args", chromeArguments = "--user-agent=keycloak-test-framework-test")
    ManagedWebDriver driver;

    @Test
    public void chromeArgumentsAreApplied() {
        Assumptions.assumeTrue(driver.getBrowserType() == BrowserType.CHROME);
        String userAgent = (String) ((JavascriptExecutor) driver.driver()).executeScript("return navigator.userAgent");
        assertTrue(userAgent.startsWith("keycloak-test-framework-test"), "Unexpected user agent: " + userAgent);
    }
}
