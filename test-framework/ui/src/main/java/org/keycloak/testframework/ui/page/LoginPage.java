package org.keycloak.testframework.ui.page;

import java.util.Optional;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginPage extends AbstractLoginPage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(css = "[type=submit]")
    private WebElement submitButton;

    @FindBy(id = "rememberMe")
    private WebElement rememberMe;

    @FindBy(linkText = "Register")
    private WebElement registerLink;

    @FindBy(linkText = "Forgot Password?")
    private WebElement resetPasswordLink;

    @FindBy(className = "pf-m-success")
    private WebElement loginSuccessMessage;

    @FindBy(id = "input-error-username")
    private WebElement userNameInputError;

    @FindBy(id = "input-error-password")
    private WebElement passwordInputError;

    public LoginPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void fillLogin(String username, String password) {
        usernameInput.clear();
        usernameInput.sendKeys(username);
        passwordInput.clear();
        passwordInput.sendKeys(password);
    }

    public void fillPassword(String password) {
        passwordInput.clear();
        passwordInput.sendKeys(password);
    }

    public void submit() {
        submitButton.click();
    }

    public void clickSocial(String alias) {
        WebElement socialButton = findSocialButton(alias);
        socialButton.click();
    }

    public WebElement findSocialButton(String alias) {
        String id = "social-" + alias;
        return driver.findElement(By.id(id));
    }

    public boolean isSocialButtonPresent(String alias) {
        String id = "social-" + alias;
        return !driver.driver().findElements(By.id(id)).isEmpty();
    }

    public void rememberMe(boolean value) {
        boolean selected = isRememberMe();
        if ((value && !selected) || !value && selected) {
            rememberMe.click();
        }
    }

    public boolean isRememberMe() {
        return rememberMe.isSelected();
    }

    public void clickRegister() {
        registerLink.click();
    }

    public void resetPassword() {
        resetPasswordLink.click();
    }

    public String getSuccessMessage() {
        return loginSuccessMessage != null ? loginSuccessMessage.getText() : null;
    }

    @Override
    public String getExpectedPageId() {
        return "login-login";
    }

    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public String getUsernameAutocomplete() {
        return usernameInput.getDomAttribute("autocomplete");
    }

    public void clearUsernameInput() {
        usernameInput.clear();
    }

    public String getUsernameInputError() {
        try {
            return userNameInputError.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public Optional<String> getPasswordInputError() {
        try {
            return Optional.of(passwordInputError.getText());
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }
}
