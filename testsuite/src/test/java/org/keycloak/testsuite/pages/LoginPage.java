package org.keycloak.testsuite.pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginPage extends Page {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(linkText = "Register")
    private WebElement registerLink;

    @FindBy(linkText = "Reset password")
    private WebElement resetPasswordLink;

    @FindBy(id = "loginError")
    private WebElement loginErrorMessage;

    public void login(String username, String password) {
        usernameInput.clear();
        usernameInput.sendKeys(username);

        passwordInput.clear();
        passwordInput.sendKeys(password);

        submitButton.click();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    public boolean isCurrent() {
        return driver.getTitle().equals("Log in to test");
    }

    public void clickRegister() {
        registerLink.click();
    }

    public void resetPassword() {
        resetPasswordLink.click();
    }

    @Override
    public void open() {
        oauth.openLoginForm();
        assertCurrent();
    }

}
