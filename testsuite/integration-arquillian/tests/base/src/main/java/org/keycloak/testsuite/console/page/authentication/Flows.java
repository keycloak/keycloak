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

    public void changeFlowSelect(FlowSelectValues value) {
        flowSelect.selectByVisibleText(value.getName());
    }

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

    @FindBy(linkText = "New")
    private WebElement newButton;

    @FindBy(linkText = "Copy")
    private WebElement copyButton;

    public void clickNew() {
        newButton.click();
    }

    public void clickCopy() {
        copyButton.click();
    }

    // Direct grant
    public void setPasswordRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Password')]]/../td[2]//input[@type='radio']")).click();
    }

    public void setPasswordDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Password')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setOTPRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'O T P')]]/../td[2]//input[@type='radio']")).click();
    }

    public void setOTPOptional() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'O T P')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setOTPDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'O T P')]]/../td[4]//input[@type='radio']")).click();
    }

    // Registration
    public void setRegistrationFormRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Registration form')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setRegistrationFormDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Registration form')]]/../td[4]//input[@type='radio']")).click();
    }

    public void setRegistrationUserCreationRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Registration  User  Creation')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setRegistrationUserCreationDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Registration  User  Creation')]]/../td[4]//input[@type='radio']")).click();
    }

    public void setProfileValidationRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Profile  Validation')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setProfileValidationDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Profile  Validation')]]/../td[4]//input[@type='radio']")).click();
    }

    public void setPasswordValidationRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Password  Validation')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setPasswordValidationDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Password  Validation')]]/../td[4]//input[@type='radio']")).click();
    }

    public void setRecaptchaRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Recaptcha')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setRecaptchaDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Recaptcha')]]/../td[4]//input[@type='radio']")).click();
    }

    // Browser
    public void setCookieAlternative() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Cookie')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setCookieDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Cookie')]]/../td[4]//input[@type='radio']")).click();
    }

    public void setKerberosAlternative() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Kerberos')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setKerberosRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Kerberos')]]/../td[4]//input[@type='radio']")).click();
    }

    public void setKerberosDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Kerberos')]]/../td[5]//input[@type='radio']")).click();
    }

    public void setFormsAlternative() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Forms')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setFormsRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Forms')]]/../td[4]//input[@type='radio']")).click();
    }

    public void setFormsDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Forms')]]/../td[5]//input[@type='radio']")).click();
    }

    public void setOTPFormRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,' O T P  Form')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setOTPFormOptional() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,' O T P  Form')]]/../td[4]//input[@type='radio']")).click();
    }

    public void setOTPFormDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,' O T P  Form')]]/../td[5]//input[@type='radio']")).click();
    }

    // Reset credentials
    public void setResetPasswordRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Reset  Password')]]/../td[2]//input[@type='radio']")).click();
    }

    public void setResetPasswordOptional() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Reset  Password')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setResetPasswordDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Reset  Password')]]/../td[4]//input[@type='radio']")).click();
    }

    public void setResetOTPRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Reset  O T P')]]/../td[2]//input[@type='radio']")).click();
    }

    public void setResetOTPOptional() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Reset  O T P')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setResetOTPDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Reset  O T P')]]/../td[4]//input[@type='radio']")).click();
    }

    // Clients
    public void setClientIdAndSecretRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Client  Id and  Secret')]]/../td[2]//input[@type='radio']")).click();
    }

    public void setClientIdAndSecretAlternative() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Client  Id and  Secret')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setClientIdAndSecretDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,'Client  Id and  Secret')]]/../td[4]//input[@type='radio']")).click();
    }

    public void setSignedJwtRequired() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,' Signed  Jwt')]]/../td[2]//input[@type='radio']")).click();
    }

    public void setSignedJwtAlternative() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,' Signed  Jwt')]]/../td[3]//input[@type='radio']")).click();
    }

    public void setSignedJwtDisabled() {
        driver.findElement(By.xpath("//td[@class='ng-binding' and text()[contains(.,' Signed  Jwt')]]/../td[4]//input[@type='radio']")).click();
    }
}
