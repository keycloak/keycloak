package org.keycloak.testsuite.util;


import org.jboss.logging.Logger;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.regex.Pattern;

import static org.openqa.selenium.support.ui.ExpectedConditions.*;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class URLUtils {

    public static void navigateToUri(WebDriver driver, String uri, boolean waitForMatch) {
        navigateToUri(driver, uri, waitForMatch, true);
    }

    private static void navigateToUri(WebDriver driver, String uri, boolean waitForMatch, boolean enableIEWorkaround) {
        Logger log = Logger.getLogger(URLUtils.class);

        log.info("starting navigation");

        // In IE, sometime the current URL is not correct; one of the indicators is that the target URL
        // equals the current URL
        if (driver instanceof InternetExplorerDriver && driver.getCurrentUrl().equals(uri)) {
            log.info("IE workaround: target URL equals current URL - refreshing the page");
            driver.navigate().refresh();
        }

        WaitUtils.waitForPageToLoad(driver);

        log.info("current URL:  " + driver.getCurrentUrl());
        log.info("navigating to " + uri);
        driver.navigate().to(uri);

        if (waitForMatch) {
            // Possible login URL; this is to eliminate unnecessary wait when navigating to a secured page and being
            // redirected to the login page
            String loginUrl = "^[^\\?]+/auth/realms/[^/]+/(protocol|login-actions).+$";

            try {
                (new WebDriverWait(driver, 3)).until(or(urlMatches("^" + Pattern.quote(uri) + ".*$"), urlMatches(loginUrl)));
            } catch (TimeoutException e) {
                log.info("new current URL doesn't start with desired URL");
            }
        }

        WaitUtils.waitForPageToLoad(driver);

        log.info("new current URL:  " + driver.getCurrentUrl());

        // In IE, after deleting the cookies for test realm, the first loaded page in master's admin console
        // contains invalid URL (misses #/realms/[realm] or contains state and code fragments), although the
        // address bar states the correct URL; seemingly this is another bug in IE WebDriver)
        if (enableIEWorkaround && driver instanceof InternetExplorerDriver
                && (driver.getCurrentUrl().matches("^[^#]+/#state=[^#/&]+&code=[^#/&]+$")
                ||  driver.getCurrentUrl().matches("^.+/auth/admin/[^/]+/console/$"))) {
            log.info("IE workaround: reloading the page after deleting the cookies...");
            navigateToUri(driver, uri, waitForMatch, false);
        }
        else {
            log.info("navigation complete");
        }
    }

    public static boolean currentUrlEqual(WebDriver driver, String url) {
        return urlCheck(driver, urlToBe(url));
    }

    public static boolean currentUrlDoesntEqual(WebDriver driver, String url) {
        return urlCheck(driver, not(urlToBe(url)));
    }

    public static boolean currentUrlStartWith(WebDriver driver, String url) {
        return urlCheck(driver, urlMatches("^" + Pattern.quote(url) + ".*$"));
    }

    public static boolean currentUrlDoesntStartWith(WebDriver driver, String url) {
        return urlCheck(driver, urlMatches("^(?!" + Pattern.quote(url) + ").+$"));
    }

    private static boolean urlCheck(WebDriver driver, ExpectedCondition condition) {
        return urlCheck(driver, condition, false);
    }

    private static boolean urlCheck(WebDriver driver, ExpectedCondition condition, boolean secondTry) {
        Logger log = Logger.getLogger(URLUtils.class);

        try {
            (new WebDriverWait(driver, 1, 100)).until(condition);
        }
        catch (TimeoutException e) {
            if (driver instanceof InternetExplorerDriver && !secondTry) {
                // IE WebDriver has sometimes invalid current URL
                log.info("IE workaround: checking URL failed at first attempt - refreshing the page and trying one more time...");
                driver.navigate().refresh();
                urlCheck(driver, condition, true);
            }
            else {
                return false;
            }
        }
        return true;
    }

}
