package org.keycloak.testsuite.pages;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginPage {

    @Drone
    private WebDriver browser;

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(linkText = "Register")
    private WebElement registerLink;

    @FindBy(id = "loginError")
    private WebElement loginErrorMessage;

    public void login(String username, String password) {
        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);

        submitButton.click();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    public boolean isCurrent() {
        return browser.getTitle().equals("Log in to demo");
    }

    public void register() {
        registerLink.click();
    }

}
