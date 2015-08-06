package org.keycloak.testsuite.page;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import static org.keycloak.testsuite.util.SeleniumUtils.waitGuiForElementPresent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 *
 * @author tkyjovsk
 */
public class Form {

    @Drone
    protected WebDriver driver;
    
    @FindByJQuery("button[kc-save] ")
    private WebElement save;
    @FindByJQuery("button[kc-cancel] ")
    private WebElement cancel;

    public void save() {
        save.click();
    }

    public void cancel() {
        cancel.click();
    }

    public static void setInputText(WebElement input, String text) {
        waitGuiForElementPresent(input, "Required input element not present.");
        if (input.isEnabled()) {
            input.clear();
            if (text != null) {
                input.sendKeys(text);
            }
        } else {
            // TODO log warning
        }
    }

}
