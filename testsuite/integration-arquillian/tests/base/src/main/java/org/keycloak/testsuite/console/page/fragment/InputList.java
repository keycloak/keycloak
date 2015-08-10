package org.keycloak.testsuite.console.page.fragment;

import java.util.ArrayList;
import java.util.List;
import static org.keycloak.testsuite.page.Form.getInputValue;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class InputList {
    
    @FindBy(xpath=".//input[@ng-model='client.redirectUris[i]']")
    private List<WebElement> inputs;
    
    public List<String> getValues() {
        List<String> values = new ArrayList<>();
        for (WebElement input: inputs) {
            values.add(getInputValue(input));
        }
        return values;
    }
    
    
    
}
