package org.keycloak.testsuite.pages.social;

import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.WaitUtils.pause;

public class OrcidLoginPage extends AbstractSocialLoginPage {
    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "signin-button")
    private WebElement loginButton;

    @Override
    public void login(String user, String password) {
        usernameInput.clear();
        usernameInput.sendKeys(user);
        passwordInput.sendKeys(password);
        loginButton.click();
    }
}