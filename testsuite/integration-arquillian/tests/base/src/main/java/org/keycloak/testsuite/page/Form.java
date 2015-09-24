package org.keycloak.testsuite.page;

import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import org.jboss.logging.Logger;
import static org.keycloak.testsuite.util.WaitUtils.waitAjaxForElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class Form {

    protected final Logger log = Logger.getLogger(this.getClass());
    
    @Drone
    protected WebDriver driver;

    public static final String ACTIVE_DIV_XPATH = ".//div[not(contains(@class,'ng-hide'))]";

    @FindBy(xpath = ACTIVE_DIV_XPATH + "/button[text()='Save']")
    private WebElement save;
    @FindBy(xpath = ACTIVE_DIV_XPATH + "/button[text()='Cancel']")
    private WebElement cancel;

    public void save() {
//        guardAjax(save).click();
        save.click();
    }

    public void cancel() {
        guardAjax(cancel).click();
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
