package org.keycloak.testsuite.pages;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class RegisterPage {

    @Drone
    private WebDriver browser;

    @FindBy(id = "name")
    private WebElement nameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-confirm")
    private WebElement passwordConfirmInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(id = "loginError")
    private WebElement loginErrorMessage;

    public void register(String name, String email, String username, String password, String passwordConfirm) {
        if (name != null) {
            nameInput.sendKeys(name);
        }

        if (email != null) {
            emailInput.sendKeys(email);
        }

        if (username != null) {
            usernameInput.sendKeys(username);
        }

        if (password != null) {
            passwordInput.sendKeys(password);
        }

        if (passwordConfirm != null) {
            passwordConfirmInput.sendKeys(passwordConfirm);
        }

        submitButton.click();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    public boolean isCurrent() {
        return browser.getTitle().equals("Register with demo");
    }

}
