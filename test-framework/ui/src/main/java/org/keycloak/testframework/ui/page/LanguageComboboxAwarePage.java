package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

public final class LanguageComboboxAwarePage {

    private LanguageComboboxAwarePage() {
    }

    public static void assertAttemptedUsernameAvailability(ManagedWebDriver driver, boolean expectedAvailability) {
        assertAttemptedUsernameAvailability(driver.driver(), expectedAvailability);
    }

    public static void assertAttemptedUsernameAvailability(WebDriver driver, boolean expectedAvailability) {
        try {
            driver.findElement(By.id("kc-attempted-username"));
            Assertions.assertTrue(expectedAvailability);
            Assertions.assertTrue(driver.findElements(By.id("username")).isEmpty());
        } catch (NoSuchElementException nse) {
            Assertions.assertFalse(expectedAvailability);
        }
    }
}
