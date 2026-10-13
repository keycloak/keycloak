package org.keycloak.testsuite.util;


import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.jboss.logging.Logger;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.testsuite.util.DroneUtils.getCurrentDriver;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlToBe;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class URLUtils {

    private static Logger log = Logger.getLogger(URLUtils.class);

    public static void navigateToUri(String uri) {
        WebDriver driver = getCurrentDriver();

        log.info("starting navigation");

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
        log.info("navigation complete");
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

    @SuppressWarnings("unchecked")
    private static boolean urlCheck(ExpectedCondition condition) {
        WebDriver driver = getCurrentDriver();

        try {
            (new WebDriverWait(driver, Duration.ofSeconds(5), Duration.ofMillis(100))).until(condition);
        } catch (TimeoutException e) {
            return false;
        }
        return true;
    }

    /**
     * @return action-url from the HTML code of the current page. Assumption is, that page is one of the Keycloak login pages (login theme pages)
     */
    public static String getActionUrlFromCurrentPage(WebDriver driver) {
        Matcher m = Pattern.compile("form action=\"([^\"]*)\"").matcher(driver.getPageSource());
        if (m.find()) {
            return m.group(1);
        } else {
            return null;
        }
    }

    /**
     * @see #sendPOSTRequestWithWebDriver(String, String)
     *
     * @param postRequestUrl
     * @param formParams form params in key/value form
     */
    public static void sendPOSTRequestWithWebDriver(String postRequestUrl, Map<String, String> formParams) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : formParams.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append("&");
            }
            sb.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
        }
        sendPOSTRequestWithWebDriver(postRequestUrl, sb.toString());
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
