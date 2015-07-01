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

    private final Map<String, Object> templateValues = new HashMap<>();

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
        templateValues.put(template, value);
        return this;
    }

    public URL getUrl() {
        try {
            return createUriBuilder().buildFromMap(templateValues).toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Page URL is malformed.");
        }
    }

    public String getUrlString() {
        return getUrl().toExternalForm();
    }

    public void navigateTo() {
        navigateToUsing(driver);
    }

    public void navigateToUsing(WebDriver driver) {
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
