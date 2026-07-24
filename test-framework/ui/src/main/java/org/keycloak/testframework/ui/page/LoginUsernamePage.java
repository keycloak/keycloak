package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginUsernamePage extends AbstractLoginPage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(css = "[type=submit]")
    private WebElement submitButton;

    @FindBy(id = "input-error-username")
    private WebElement userNameInputError;

    @FindBy(id = "rememberMe")
    private WebElement rememberMe;

    public LoginUsernamePage(ManagedWebDriver driver) {
        super(driver);
    }

    public void fillLoginWithUsernameOnly(String username) {
        usernameInput.clear();
        usernameInput.sendKeys(username);
    }

    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public String getUsernameAutocomplete() {
        return usernameInput.getDomAttribute("autocomplete");
    }

    public String getUsernameInputError() {
        try {
            return userNameInputError.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public void submit() {
        submitButton.click();
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-username";
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

    public boolean isSocialButtonPresent(String alias) {
        String id = "social-" + alias;
        return !driver.driver().findElements(By.id(id)).isEmpty();
    }

    public void clickSocial(String alias) {
        WebElement socialButton = findSocialButton(alias);
        socialButton.click();
    }

    public WebElement findSocialButton(String alias) {
        String id = "social-" + alias;
        return driver.findElement(By.id(id));
    }
}
