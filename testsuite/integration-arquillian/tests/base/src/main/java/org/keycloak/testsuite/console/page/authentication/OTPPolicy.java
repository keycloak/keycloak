package org.keycloak.testsuite.console.page.authentication;

import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Created by mhajas on 8/21/15.
 */
public class OTPPolicy extends Authentication {

    @FindBy(linkText = "Save")
    private WebElement saveButton;

    public void clickSave() {
        saveButton.click();
    }

    @FindBy(linkText = "Cancel")
    private WebElement cancelButton;

    @FindBy(id = "lookAhead")
    private WebElement lookAheadInput;

    @FindBy(id = "counter")
    private WebElement initialCounterInput;

    public void clickCancel() {
        cancelButton.click();
    }

    public void setLookAheadInputValue(String value) {
        Form.setInputValue(lookAheadInput, value);
    }

    public void setInitialcounterInputValue(String value) {
        Form.setInputValue(initialCounterInput, value);
    }

    public enum OTPTypeSelectValues {

        TIME_BASED("time Based"), COUNTER_BASED("Counter Based");

        private String name;

        private OTPTypeSelectValues(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum OTPHashAlgorithmSelectValues {

        SHA1("SHA1"), SHA256("SHA256"), SHA512("SHA512");

        private String name;

        private OTPHashAlgorithmSelectValues(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum NumberOfDigitsSelectValues {

        NUMBER6("6"), NUMBER8("8");

        private String name;

        private NumberOfDigitsSelectValues(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
