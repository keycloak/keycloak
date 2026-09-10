package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginPasswordResetPage extends AbstractLoginPage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(css = "[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(id = "kc-reset-password-form")
    private WebElement formResetPassword;

    @FindBy(partialLinkText = "Back to Login")
    private WebElement backToLogin;

    public LoginPasswordResetPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void changePassword(String username) {
        usernameInput.clear();
        usernameInput.sendKeys(username);

        submitButton.click();
    }

    public void backToLogin() {
        backToLogin.click();
    }

    public String getFormUrl() {
        return formResetPassword.getAttribute("action");
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-reset-password";
    }

}
