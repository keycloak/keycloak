package org.keycloak.testsuite.page;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.openqa.selenium.WebElement;

/**
 *
 * @author tkyjovsk
 */
public class Form {

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

    public void setInputText(WebElement input, String text) {
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
