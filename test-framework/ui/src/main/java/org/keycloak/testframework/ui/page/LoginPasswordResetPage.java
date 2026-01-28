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

    public LoginPasswordResetPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void changePassword(String username) {
        usernameInput.clear();
        usernameInput.sendKeys(username);

        submitButton.click();
    }

    public String getFormUrl() {
        return formResetPassword.getAttribute("action");
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-reset-password";
    }

}
