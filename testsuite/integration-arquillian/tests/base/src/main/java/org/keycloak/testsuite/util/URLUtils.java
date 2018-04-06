package org.keycloak.testsuite.util;


import org.jboss.logging.Logger;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.regex.Pattern;

import static org.keycloak.testsuite.util.DroneUtils.getCurrentDriver;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlToBe;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class URLUtils {

    private static Logger log = Logger.getLogger(URLUtils.class);

    public static void navigateToUri(String uri, boolean waitForMatch) {
        navigateToUri(uri, waitForMatch, true);
    }

    // TODO: remove waitForMatch
    private static void navigateToUri(String uri, boolean waitForMatch, boolean enableIEWorkaround) {
        WebDriver driver = getCurrentDriver();

        log.info("starting navigation");

        // In IE, sometime the current URL is not correct; one of the indicators is that the target URL
        // equals the current URL
        if (driver instanceof InternetExplorerDriver && driver.getCurrentUrl().equals(uri)) {
            log.info("IE workaround: target URL equals current URL - refreshing the page");
            driver.navigate().refresh();
            waitForPageToLoad();
        }

        log.info("current URL:  " + driver.getCurrentUrl());
        log.info("navigating to " + uri);
        if (driver.getCurrentUrl().equals(uri)) { // Some browsers won't do anything if navigating to the same URL; this "fixes" it
            log.info("target URL equals current URL - refreshing the page");
            driver.navigate().refresh();
        }
        else {
            driver.navigate().to(uri);
        }

        waitForPageToLoad();

        log.info("new current URL:  " + driver.getCurrentUrl());

        // In IE, after deleting the cookies for test realm, the first loaded page in master's admin console
        // contains invalid URL (misses #/realms/[realm] or contains state and code fragments), although the
        // address bar states the correct URL; seemingly this is another bug in IE WebDriver)
        if (enableIEWorkaround && driver instanceof InternetExplorerDriver
                && (driver.getCurrentUrl().matches("^[^#]+/#state=[^#/&]+&code=[^#/&]+$")
                ||  driver.getCurrentUrl().matches("^.+/auth/admin/[^/]+/console/$"))) {
            log.info("IE workaround: reloading the page after deleting the cookies...");
            navigateToUri(uri, waitForMatch, false);
        }
        else {
            log.info("navigation complete");
        }
    }

    public static boolean currentUrlEqual(String url) {
        return urlCheck(urlToBe(url));
    }

    public static boolean currentUrlDoesntEqual(String url) {
        return urlCheck(not(urlToBe(url)));
    }

    public static boolean currentUrlStartWith(String url) {
        return urlCheck(urlMatches("^" + Pattern.quote(url) + ".*$"));
    }

    public static boolean currentUrlDoesntStartWith(String url) {
        return urlCheck(urlMatches("^(?!" + Pattern.quote(url) + ").+$"));
    }

    private static boolean urlCheck(ExpectedCondition condition) {
        return urlCheck(condition, false);
    }

    private static boolean urlCheck(ExpectedCondition condition, boolean secondTry) {
        WebDriver driver = getCurrentDriver();

        try {
            (new WebDriverWait(driver, 5, 100)).until(condition);
        }
        catch (TimeoutException e) {
            if (driver instanceof InternetExplorerDriver && !secondTry) {
                // IE WebDriver has sometimes invalid current URL
                log.info("IE workaround: checking URL failed at first attempt - refreshing the page and trying one more time...");
                driver.navigate().refresh();
                urlCheck(condition, true);
            }
            else {
                return false;
            }
        }
        return true;
    }

}
