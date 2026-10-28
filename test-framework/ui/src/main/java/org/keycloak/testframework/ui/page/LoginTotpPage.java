package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginTotpPage extends AbstractLoginPage {

    @FindBy(id = "otp")
    private WebElement otpInput;

    @FindBy(name = "login")
    private WebElement submitButton;

    public LoginTotpPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void login(String otp) {
        otpInput.clear();
        otpInput.sendKeys(otp);
        submitButton.click();
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-otp";
    }
}
