package org.keycloak.testsuite;

import org.keycloak.testsuite.page.AbstractPage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author tkyjovsk
 */
public class PageAssert {

    public static void assertCurrentUrl(AbstractPage page) {
        assertCurrentUrl(page.getDriver(), page.getUrlString());
    }

    public static void assertCurrentUrl(WebDriver driver, String url) {
        assertEquals(driver.getCurrentUrl(), url);
    }

    public static void assertCurrentUrlStartsWith(AbstractPage page) {
        assertCurrentUrlStartsWith(page.getDriver(), page.getUrlString());
    }

    public static void assertCurrentUrlStartsWith(WebDriver driver, String url) {
        assertTrue(driver.getCurrentUrl().startsWith(url));
    }

    public static void assertCurrentUrlDoesntStartWith(AbstractPage page) {
        assertCurrentUrlDoesntStartWith(page.getDriver(), page.getUrlString());
    }

    public static void assertCurrentUrlDoesntStartWith(WebDriver driver, String url) {
        assertFalse(driver.getCurrentUrl().startsWith(url));
    }

}
