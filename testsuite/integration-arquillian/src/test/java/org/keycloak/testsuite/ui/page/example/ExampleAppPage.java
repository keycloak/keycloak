package org.keycloak.testsuite.ui.page.example;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Assert;
import static org.keycloak.testsuite.ui.util.URL.*;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author tkyjovsk
 */
public class ExampleAppPage {
    
    @Drone
    protected WebDriver driver;

    private final String url;

    public ExampleAppPage(String exampleAppURLSuffix) {
        this.url = APP_SERVER_BASE_URL + exampleAppURLSuffix;
    }

    public String getUrl() {
        return url;
    }
    
    public void open() {
        driver.get(url);
    }
    
    public void assertCurrentURL() {
        Assert.assertTrue(driver.getCurrentUrl().startsWith(url));
    }
    
}
