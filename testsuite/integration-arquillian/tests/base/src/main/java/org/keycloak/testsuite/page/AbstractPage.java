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

    public String getFragment() {
        return "";
    }

    public UriBuilder getUriBuilder() {
        if (builder == null) {
            builder = createUriBuilder();
            String fragment = getFragment();
            if (fragment != null && !fragment.isEmpty()) {
                builder.fragment(fragment);
            }
        }
        return builder;
    }

    public AbstractPage setTemplateValue(String template, Object value) {
        templateValues.put(template, value);
        return this;
    }

    public Object getTemplateValue(String template) {
        return templateValues.get(template);
    }

    public URI getUri() {
        return getUriBuilder().buildFromMap(templateValues);
    }

    public AbstractPage navigateTo() {
        String uri = getUri().toASCIIString();
        System.out.println("navigating to " + uri);
        driver.navigate().to(uri);
        return this;
    }

    public WebDriver getDriver() {
        return driver;
    }

    @Override
    public String toString() {
        return getUri().toASCIIString();
    }

}
