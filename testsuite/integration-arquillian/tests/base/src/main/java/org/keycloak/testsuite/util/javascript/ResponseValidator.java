package org.keycloak.testsuite.util.javascript;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.Serializable;
import java.util.Map;

/**
 * @author mhajas
 */
public interface ResponseValidator extends Serializable {

    void validate(Map<String, Object> response);
}
