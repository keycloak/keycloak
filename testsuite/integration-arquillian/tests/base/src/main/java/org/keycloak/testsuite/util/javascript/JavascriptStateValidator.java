package org.keycloak.testsuite.util.javascript;

import org.keycloak.models.KeycloakSession;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.Serializable;

/**
 * @author mhajas
 */
public interface JavascriptStateValidator extends Serializable {

    void validate(WebDriver driver, Object output, WebElement events);
}
