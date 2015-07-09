package org.keycloak.testsuite.page;

import java.net.URI;
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

    public URI getUri() {
        return createUriBuilder().buildFromMap(this.defaultTemplateValues);
    }

    public URI getUri(Map<String, Object> templateValues) {
        return createUriBuilder().buildFromMap(templateValues);
    }

    public void navigateTo() {
        navigateToUsing(driver);
    }

    public void navigateToUsing(WebDriver driver) {
        System.out.println("navigating to " + getUri().toASCIIString());
        driver.get(getUri().toASCIIString());
    }

    public void navigateToUsingSecondBrowser(
            @Drone @SecondBrowser WebDriver driver2) {
        driver2.navigate().to(getUri().toASCIIString());
    }

    public WebDriver getDriver() {
        return driver;
    }

    @Override
    public String toString() {
        return getUri().toASCIIString();
    }

}
