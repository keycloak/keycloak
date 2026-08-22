package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginTotpPage extends AbstractLoginPage {

    @FindBy(id = "otp")
    private WebElement otpInput;

    @FindBy(css = "[type=submit]")
    private WebElement submitButton;

    @FindBy(id = "input-error-otp-code")
    private WebElement inputError;

    public LoginTotpPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void login(String totp) {
        otpInput.clear();
        otpInput.sendKeys(totp);
        submitButton.click();
    }

    public String getInputError() {
        try {
            return inputError.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getAlertError() {
        return getErrorMessage().orElse(null);
    }

    @Override
    public String getExpectedPageId() {
        return "login-otp";
    }
}
