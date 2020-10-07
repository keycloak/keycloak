package org.keycloak.testsuite.util;


import org.jboss.logging.Logger;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.keycloak.testsuite.util.DroneUtils.getCurrentDriver;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;
import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlToBe;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class URLUtils {

    private static Logger log = Logger.getLogger(URLUtils.class);

    public static void navigateToUri(String uri) {
        navigateToUri(uri, true);
    }

    private static void navigateToUri(String uri, boolean enableIEWorkaround) {
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
            navigateToUri(uri, false);
        }
        else {
            log.info("navigation complete");
        }
    }

    public static boolean currentUrlEquals(String url) {
        return urlCheck(urlToBe(removeDefaultPorts(url)));
    }

    public static boolean currentUrlDoesntEqual(String url) {
        return urlCheck(not(urlToBe(url)));
    }

    public static boolean currentUrlWithQueryEquals(String expectedUrl, String... expectedQuery) {
        List<String> expectedQueryList = Arrays.asList(expectedQuery);

        ExpectedCondition<Boolean> condition = (WebDriver driver) -> {
            String[] urlParts = driver.getCurrentUrl().split("\\?", 2);
            if (urlParts.length != 2) {
                throw new RuntimeException("Current URL doesn't contain query string");
            }
            List<String> queryParts = Arrays.asList(urlParts[1].split("&"));

            return urlParts[0].equals(expectedUrl) && queryParts.containsAll(expectedQueryList);
        };

        return urlCheck(condition);
    }

    public static boolean currentUrlStartsWith(String url) {
        return currentUrlMatches("^" + Pattern.quote(removeDefaultPorts(url)) + ".*$");
    }

    public static boolean currentUrlDoesntStartWith(String url) {
        return currentUrlMatches("^(?!" + Pattern.quote(removeDefaultPorts(url)) + ").+$");
    }

    public static boolean currentUrlMatches(String regex) {
        return urlCheck(urlMatches(regex));
    }

    private static boolean urlCheck(ExpectedCondition condition) {
        return urlCheck(condition, false);
    }

    @SuppressWarnings("unchecked")
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

    /**
     * This will send POST request to specified URL with specified form parameters. It's not easily possible to "trick" web driver to send POST
     * request with custom parameters, which are not directly available in the form.
     *
     * See URLUtils.sendPOSTWithWebDriver for more details
     *
     * @param postRequestUrl Absolute URL. It can include query parameters etc. The POST request will be send to this URL
     * @param encodedFormParameters Encoded parameters in the form of "param1=value1&param2=value2"
     * @return
     */
    public static void sendPOSTRequestWithWebDriver(String postRequestUrl, String encodedFormParameters) {
        WebDriver driver = getCurrentDriver();

        URI uri = KeycloakUriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT + "/realms/master/testing/simulate-post-request")
                .queryParam("postRequestUrl", postRequestUrl)
                .queryParam("encodedFormParameters", encodedFormParameters)
                .build();

        driver.navigate().to(uri.toString());
    }

}
