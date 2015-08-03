package org.keycloak.testsuite.util;

import org.openqa.selenium.WebElement;

/**
 *
 * @author tkyjovsk
 */
public class Forms {

    public static void setCheckboxValue(WebElement checkbox, boolean selected) {
        if ((selected && !checkbox.isSelected())
                || (!selected && checkbox.isSelected())) {
            checkbox.click();
        }
    }
    
}
