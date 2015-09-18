package org.keycloak.testsuite.util;

import javax.ws.rs.core.UriBuilder;
import org.keycloak.testsuite.page.AbstractPage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.keycloak.testsuite.auth.page.login.PageWithLoginUrl;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author tkyjovsk
 */
public class URLAssert {

    public static void assertCurrentUrlEquals(AbstractPage page) {
        assertCurrentUrlEquals(page.getDriver(), page);
    }

    public static void assertCurrentUrlEquals(WebDriver driver, final AbstractPage page) {
//        WebDriverWait wait = new WebDriverWait(driver, 1);
//        ExpectedCondition<Boolean> urlStartsWith = new ExpectedCondition<Boolean>() {
//
//            @Override
//            public Boolean apply(WebDriver wd) {
//                return startsWithNormalized(wd.getCurrentUrl(), page.toString());
//            }
//        };
//        wait.until(urlStartsWith);
        assertEqualsNormalized(driver.getCurrentUrl(), page.toString());
    }

    public static void assertCurrentUrlStartsWith(AbstractPage page) {
        assertCurrentUrlStartsWith(page.getDriver(), page.toString());
    }

    public static void assertCurrentUrlStartsWith(WebDriver driver, final String url) {
//        WebDriverWait wait = new WebDriverWait(driver, 1);
//        ExpectedCondition<Boolean> urlStartsWith = new ExpectedCondition<Boolean>() {
//
//            @Override
//            public Boolean apply(WebDriver wd) {
//                return startsWithNormalized(wd.getCurrentUrl(), url);
//            }
//        };
//        wait.until(urlStartsWith);
        assertTrue(startsWithNormalized(driver.getCurrentUrl(), url));
    }

    public static void assertCurrentUrlDoesntStartWith(AbstractPage page) {
        assertCurrentUrlDoesntStartWith(page.getDriver(), page.toString());
    }

    public static void assertCurrentUrlDoesntStartWith(WebDriver driver, final String url) {
//        WebDriverWait wait = new WebDriverWait(driver, 1, 250);
//        ExpectedCondition<Boolean> urlDoesntStartWith = new ExpectedCondition<Boolean>() {
//
//            @Override
//            public Boolean apply(WebDriver wd) {
//                return !startsWithNormalized(wd.getCurrentUrl(), url);
//            }
//        };
//        wait.until(urlDoesntStartWith);
        assertFalse(startsWithNormalized(driver.getCurrentUrl(), url));
    }

    // this normalization is needed because of slash-encoding in uri fragment (the part after #)
    public static String normalizeUri(String uri) {
        return UriBuilder.fromUri(uri).build().toASCIIString();
    }

    public static boolean startsWithNormalized(String str1, String str2) {
        String uri1 = normalizeUri(str1);
        String uri2 = normalizeUri(str2);
        return uri1.startsWith(uri2);
    }

    public static void assertEqualsNormalized(String str1, String str2) {
        assertEquals(normalizeUri(str1), normalizeUri(str2));
    }



    public static void assertCurrentUrlStartsWithLoginUrlOf(PageWithLoginUrl page) {
        assertCurrentUrlStartsWithLoginUrlOf(page.getDriver(), page);
    }
    
    public static void assertCurrentUrlStartsWithLoginUrlOf(WebDriver driver, PageWithLoginUrl page) {
        assertCurrentUrlStartsWith(driver, page.getOIDCLoginUrl().toString());
    }

}
