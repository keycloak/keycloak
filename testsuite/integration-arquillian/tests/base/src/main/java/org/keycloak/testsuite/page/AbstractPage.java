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

    private final Map<String, Object> uriParameters = new HashMap<>();

    @Drone
    protected WebDriver driver;

    private UriBuilder builder;

    public abstract UriBuilder createUriBuilder();

    public String getUriFragment() {
        return "";
    }

    public UriBuilder getUriBuilder() {
        if (builder == null) {
            builder = createUriBuilder();
            String fragment = getUriFragment();
            if (fragment != null && !fragment.isEmpty()) {
                builder.fragment(fragment);
            }
        }
        return builder;
    }

    public AbstractPage setUriParameter(String name, Object value) {
        uriParameters.put(name, value);
        return this;
    }

    public Object getUriParameter(String name) {
        return uriParameters.get(name);
    }

    public URI buildUri() {
        return getUriBuilder().buildFromMap(uriParameters);
    }

    public AbstractPage navigateTo() {
        String uri = buildUri().toASCIIString();
        System.out.println("navigating to " + uri);
        driver.navigate().to(uri);
        return this;
    }

    public WebDriver getDriver() {
        return driver;
    }

    @Override
    public String toString() {
        return buildUri().toASCIIString();
    }

}
