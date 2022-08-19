package org.keycloak.testsuite.console.page.authentication;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author tkyjovsk
 * @author mhajas
 */
public class Flows extends Authentication {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/flows";
    }

    @FindBy(tagName = "select")
    private Select flowSelect;

    @FindBy(linkText = "New")
    private WebElement newButton;

    @FindBy(linkText = "Copy")
    private WebElement copyButton;

    @FindBy(tagName = "table")
    private WebElement flowsTable;

    public enum FlowSelectValues {

        DIRECT_GRANT("Direct grant"), REGISTRATION("Registration"), BROWSER("Browser"),
        RESET_CREDENTIALS("Reset credentials"), CLIENTS("Clients");

        private String name;

        private FlowSelectValues(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

    public void changeFlowSelect(FlowSelectValues value) {
        flowSelect.selectByVisibleText(value.getName());
    }

    public void clickNew() {
        newButton.click();
    }

    public void clickCopy() {
        copyButton.click();
    }

    private void clickRadioButton(String row, int column) {
        flowsTable.findElement(By.xpath("//td[text()[contains(.,'" + row + "')]]/../td[" + String.valueOf(column) + "]//input[@type='radio']")).click();
    }

    // Direct grant
    public void setPasswordRequired() {
        clickRadioButton("Password", 2);
    }

    public void setPasswordDisabled() {
        clickRadioButton("Password", 3);
    }

    public void setOTPRequired() {
        clickRadioButton("O T P", 2);
    }


    public void setOTPDisabled() {
        clickRadioButton("O T P", 4);
    }

    // Registration
    public void setRegistrationFormRequired() {
        clickRadioButton("Registration form", 3);
    }

    public void setRegistrationFormDisabled() {
        clickRadioButton("Registration form", 4);
    }

    public void setRegistrationUserCreationRequired() {
        clickRadioButton("Registration  User  Creation", 3);
    }

    public void setRegistrationUserCreationDisabled() {
        clickRadioButton("Registration  User  Creation", 4);
    }

    public void setProfileValidationRequired() {
        clickRadioButton("Profile  Validation", 3);
    }

    public void setProfileValidationDisabled() {
        clickRadioButton("Profile  Validation", 4);
    }

    public void setPasswordValidationRequired() {
        clickRadioButton("Password  Validation", 3);
    }

    public void setPasswordValidationDisabled() {
        clickRadioButton("Password  Validation", 4);
    }

    public void setRecaptchaRequired() {
        clickRadioButton("Recaptcha", 3);
    }

    public void setRecaptchaDisabled() {
        clickRadioButton("Recaptcha", 4);
    }

    // Browser
    public void setCookieAlternative() {
        clickRadioButton("Cookie", 3);
    }

    public void setCookieDisabled() {
        clickRadioButton("Cookie", 4);
    }

    public void setKerberosAlternative() {
        clickRadioButton("Kerberos", 3);
    }

    public void setKerberosRequired() {
        clickRadioButton("Kerberos", 4);
    }

    public void setKerberosDisabled() {
        clickRadioButton("Kerberos", 5);
    }

    public void setFormsAlternative() {
        clickRadioButton("Forms", 3);
    }

    public void setFormsRequired() {
        clickRadioButton("Forms", 4);
    }

    public void setFormsDisabled() {
        clickRadioButton("Forms", 5);
    }

    public void setOTPFormRequired() {
        clickRadioButton(" O T P  Form", 3);
    }

    public void setOTPFormDisabled() {
        clickRadioButton(" O T P  Form", 5);
    }

    // Reset credentials
    public void setResetPasswordRequired() {
        clickRadioButton("Reset  Password", 2);
    }

    public void setResetPasswordDisabled() {
        clickRadioButton("Reset  Password", 4);
    }

    public void setResetOTPRequired() {
        clickRadioButton("Reset  O T P", 2);
    }

    public void setResetOTPDisabled() {
        clickRadioButton("Reset  O T P", 4);
    }

    // Clients
    public void setClientIdAndSecretAlternative() {
        clickRadioButton("Client  Id and  Secret", 2);
    }

    public void setClientIdAndSecretDisabled() {
        clickRadioButton("Client  Id and  Secret", 3);
    }

    public void setSignedJwtAlternative() {
        clickRadioButton(" Signed  Jwt", 2);
    }

    public void setSignedJwtDisabled() {
        clickRadioButton(" Signed  Jwt", 3);
    }
}
