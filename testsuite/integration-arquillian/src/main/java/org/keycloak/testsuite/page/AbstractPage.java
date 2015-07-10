package org.keycloak.testsuite.page;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.drone.api.annotation.Drone;
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

    public AbstractPage setTemplateValue(String template, Object value) {
        templateValues.put(template, value);
        return this;
    }

    public URI getUri() {
        return getUriBuilder().buildFromMap(templateValues);
    }

    public void navigateTo() {
        navigateToUsing(driver);
    }

    public void navigateToUsing(WebDriver driver) {
        String uri = getUri().toASCIIString();
        System.out.println("navigating to " + uri);
        driver.get(uri);
    }

    public WebDriver getDriver() {
        return driver;
    }

    @Override
    public String toString() {
        return getUri().toASCIIString();
    }

}
