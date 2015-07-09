package org.keycloak.testsuite.util;

import org.keycloak.testsuite.page.AbstractPage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author tkyjovsk
 */
public class PageAssert {
	
    public static void assertCurrentUrl(AbstractPage page) {
        assertCurrentUrl(page.getDriver(), page.toString());
    }

    public static void assertCurrentUrl(WebDriver driver, final String url) {
		WebDriverWait wait = new WebDriverWait(driver, 5);
		ExpectedCondition<Boolean> urlStartsWith = new ExpectedCondition<Boolean>() {

			@Override
			public Boolean apply(WebDriver wd) {
				return wd.getCurrentUrl().equals(url);
			}
		};
		wait.until(urlStartsWith);
        assertEquals(driver.getCurrentUrl(), url);
    }

    public static void assertCurrentUrlStartsWith(AbstractPage page) {
        assertCurrentUrlStartsWith(page.getDriver(), page.toString());
    }

    public static void assertCurrentUrlStartsWith(WebDriver driver, final String url) {
		WebDriverWait wait = new WebDriverWait(driver, 10);
		ExpectedCondition<Boolean> urlStartsWith = new ExpectedCondition<Boolean>() {

			@Override
			public Boolean apply(WebDriver wd) {
				return wd.getCurrentUrl().startsWith(url);
			}
		};
		wait.until(urlStartsWith);
		assertTrue(driver.getCurrentUrl().startsWith(url));
    }

    public static void assertCurrentUrlDoesntStartWith(AbstractPage page) {
        assertCurrentUrlDoesntStartWith(page.getDriver(), page.toString());
    }

    public static void assertCurrentUrlDoesntStartWith(WebDriver driver, final String url) {
		WebDriverWait wait = new WebDriverWait(driver, 5);
		ExpectedCondition<Boolean> urlDoesntStartWith = new ExpectedCondition<Boolean>() {

			@Override
			public Boolean apply(WebDriver wd) {
				return !wd.getCurrentUrl().startsWith(url);
			}
		};
		wait.until(urlDoesntStartWith);
        assertFalse(driver.getCurrentUrl().startsWith(url));
    }

}
