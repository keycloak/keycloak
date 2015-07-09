package org.keycloak.testsuite.page;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.keycloak.testsuite.util.SecondBrowser;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractPage {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> defaultTemplateValues = new HashMap<>();
    private final Map<String, Object> helperTemplateValues = new HashMap<>();

    @Drone
    protected WebDriver driver;

    private UriBuilder builder;

    public abstract UriBuilder createUriBuilder();

    public UriBuilder getUriBuilder() {
        if (builder == null) {
            builder = createUriBuilder();
        }
        return builder;
    }

    protected AbstractPage setTemplateValue(String template, Object value) {
        defaultTemplateValues.put(template, value);
        return this;
    }

    public URL getUrl() {
        try {
            return createUriBuilder().buildFromMap(this.defaultTemplateValues).toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Page URL is malformed.");
        }
    }

    public URL getUrl(Map<String, Object> templateValues) {
        try {
            return createUriBuilder().buildFromMap(templateValues).toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Page URL is malformed.");
        }
    }

    public String getUrlString() {
        return getUrl().toExternalForm();
    }

    public String getUrlString(Map<String, Object> templateValues) {
        return getUrl(templateValues).toExternalForm();
    }

    public void navigateTo() {
        navigateToUsing(driver);
    }

    public void navigateToUsing(WebDriver driver) {
        System.out.println("navigating to " + getUrlString());
        driver.get(getUrlString());
    }

    public void navigateToUsingSecondBrowser(
            @Drone @SecondBrowser WebDriver driver2) {
        driver2.navigate().to(getUrlString());
    }

    public WebDriver getDriver() {
        return driver;
    }

    @Override
    public String toString() {
        return getUrlString();
    }

}
