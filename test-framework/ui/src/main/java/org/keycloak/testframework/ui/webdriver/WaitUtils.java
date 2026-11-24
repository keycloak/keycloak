package org.keycloak.testframework.ui.webdriver;

import java.time.Duration;
import java.util.function.Function;

import org.keycloak.OAuth2Constants;
import org.keycloak.testframework.ui.page.AbstractPage;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WaitUtils {

    private final ManagedWebDriver managed;

    WaitUtils(ManagedWebDriver managed) {
        this.managed = managed;
    }

    public WaitUtils waitForPage(AbstractPage page) {
        String expectedPageId = page.getExpectedPageId();
        try {
            createDefaultWait()
                    .ignoring(StaleElementReferenceException.class)
                    // Also ignore WebDriverException: Chrome CDP throws "Node with given id does not belong to the document"
                    // instead of StaleElementReferenceException when getCurrentPageId() polls during a page navigation.
                    .ignoring(WebDriverException.class)
                    .until(d -> expectedPageId.equals(managed.page().getCurrentPageId()));
        } catch (TimeoutException e) {
            Assertions.fail("Expected page '" + expectedPageId + "' to be loaded, but currently on page '" + managed.page().getCurrentPageId() + "' after timeout");
        }
        return this;
    }

    public WaitUtils waitForOAuthCallback() {
        waitForOAuthCallback(webdriver1 -> {
            // match whole parameter names, as the login page URL contains "session_code="
            String url = webdriver1.getCurrentUrl();
            return hasUrlParameter(url, OAuth2Constants.CODE) || hasUrlParameter(url, OAuth2Constants.ERROR);
        });
        return this;
    }

    private static boolean hasUrlParameter(String url, String name) {
        return url.contains("?" + name + "=") || url.contains("&" + name + "=") || url.contains("#" + name + "=");
    }

    public WaitUtils waitForOAuthCallback(Function<? super WebDriver, Boolean> oAuthResponseIsPresent) {
        try {
            createDefaultWait().until(oAuthResponseIsPresent);
        } catch (TimeoutException e) {
            Assertions.fail("Expected OAuth callback, but URL was '" + managed.getCurrentUrl() + "' after timeout");
        }
        return this;
    }

    public WaitUtils waitForTitle(String title) {
        createDefaultWait().until(d -> d.getTitle().equals(title));
        return this;
    }

    /**
     * Waits until the element is no longer attached to the current document, for example after a form submit
     * replaced the page. Firefox reports this as a {@link StaleElementReferenceException}, while Chrome (CDP)
     * throws a plain {@link WebDriverException} saying the node does not belong to the document.
     */
    public WaitUtils waitUntilStale(WebElement element) {
        createDefaultWait().until(d -> {
            try {
                element.isEnabled();
                return false;
            } catch (StaleElementReferenceException e) {
                return true;
            } catch (WebDriverException e) {
                if (e.getMessage() != null && e.getMessage().contains("does not belong to the document")) {
                    return true;
                }
                throw e;
            }
        });
        return this;
    }

    /**
     * Waits until the current document has finished loading, so that its page source and elements are complete.
     * WebDriver errors are ignored as the browser might still be switching documents (see {@link #waitForPage}).
     */
    public WaitUtils waitForDocumentComplete() {
        createDefaultWait()
                .ignoring(WebDriverException.class)
                .until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        return this;
    }

    public <V> V until(Function<WebDriver, V> isTrue) {
        return createDefaultWait().until(isTrue);
    }

    private WebDriverWait createDefaultWait() {
        return new WebDriverWait(managed.driver(), Duration.ofSeconds(5), Duration.ofMillis(50));
    }

}
