package org.keycloak.testframework.ui.webdriver;

import java.time.Duration;
import java.util.function.Function;

import org.keycloak.OAuth2Constants;
import org.keycloak.testframework.ui.page.AbstractPage;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WaitUtils {

    private final ManagedWebDriver managed;

    WaitUtils(ManagedWebDriver managed) {
        this.managed = managed;
    }

    public WaitUtils waitForPage(AbstractPage page) {
        String expectedPageId = page.getExpectedPageId();
        try {
            createDefaultWait().ignoring(StaleElementReferenceException.class).until(d -> expectedPageId.equals(managed.page().getCurrentPageId()));
        } catch (TimeoutException e) {
            Assertions.fail("Expected page '" + expectedPageId + "' to be loaded, but currently on page '" + managed.page().getCurrentPageId() + "' after timeout");
        }
        return this;
    }

    public WaitUtils waitForOAuthCallback() {
        waitForOAuthCallback(webdriver1 -> webdriver1.getCurrentUrl().contains(OAuth2Constants.CODE + "=") || webdriver1.getCurrentUrl().contains(OAuth2Constants.ERROR + "="));
        return this;
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

    public <V> V until(Function<WebDriver, V> isTrue) {
        return createDefaultWait().until(isTrue);
    }

    private WebDriverWait createDefaultWait() {
        return new WebDriverWait(managed.driver(), Duration.ofSeconds(5), Duration.ofMillis(50));
    }

}
