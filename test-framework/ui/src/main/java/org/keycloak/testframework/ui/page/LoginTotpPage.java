package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginTotpPage extends AbstractLoginPage {

    @FindBy(id = "otp")
    private WebElement otpInput;

    @FindBy(css = "[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement loginErrorMessage;

    @FindBy(id = "input-error-otp")
    private WebElement totpInputCodeError;

    @FindBy(id = "input-error-otp-code")
    private WebElement otpInputCodeError;

    public LoginTotpPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void login(String totp) {
        otpInput.clear();
        if (totp != null) {
            otpInput.sendKeys(totp);
        }
        submitButton.click();
    }

    public String getAlertError() {
        try {
            return loginErrorMessage.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getInputError() {
        try {
            return totpInputCodeError.getText();
        } catch (NoSuchElementException e) {
            try {
                return otpInputCodeError.getText();
            } catch (NoSuchElementException ex) {
                return null;
            }
        }
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-otp";
    }
}
