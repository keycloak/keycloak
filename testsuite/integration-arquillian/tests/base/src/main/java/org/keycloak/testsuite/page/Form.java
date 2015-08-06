package org.keycloak.testsuite.page;

import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.keycloak.testsuite.util.SeleniumUtils.waitAjaxForElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class Form {

    @Drone
    protected WebDriver driver;

    @FindBy(xpath = "//button[text()='Save']")
    private WebElement save;
    @FindBy(xpath = "//button[text()='Cancel']")
    private WebElement cancel;

    public void save() {
        save.click();
    }

    public void cancel() {
        cancel.click();
    }

    public static String getInputValue(WebElement input) {
        waitAjaxForElement(input);
        return input.getAttribute(VALUE);
    }

    public static final String VALUE = "value";

    public static void setInputValue(WebElement input, String value) {
        waitAjaxForElement(input);
        if (input.isEnabled()) {
            input.clear();
            if (value != null) {
                input.sendKeys(value);
            }
        } else {
            // TODO log warning
        }
    }

}
