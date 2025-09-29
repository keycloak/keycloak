package org.keycloak.testframework.ui.page;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginPage extends AbstractPage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(css = "[type=submit]")
    private WebElement submitButton;

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void fillLogin(String username, String password) {
        usernameInput.sendKeys(username);
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

    @Override
    public String getExpectedPageId() {
        return "login-login";
    }

    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public void clearUsernameInput() {
        usernameInput.clear();
    }
}
