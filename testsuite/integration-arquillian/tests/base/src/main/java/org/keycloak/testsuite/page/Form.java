package org.keycloak.testsuite.page;

import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.keycloak.testsuite.util.SeleniumUtils.waitGuiForElementPresent;
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

    @FindBy(xpath = ".//button[text()='Save']")
    private WebElement save;
//    @FindByJQuery("button[kc-cancel] ")
    @FindBy(xpath = ".//button[text()='Cancel']")
    private WebElement cancel;

    public void save() {
        save.click();
//        save.sendKeys(Keys.RETURN);
    }

    public void cancel() {
        cancel.click();
//        cancel.sendKeys(Keys.RETURN);
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
