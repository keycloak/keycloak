package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginUsernamePage extends AbstractLoginPage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(css = "[type=submit]")
    private WebElement submitButton;

    public LoginUsernamePage(ManagedWebDriver driver) {
        super(driver);
    }

    public void fillLoginWithUsernameOnly(String username) {
        usernameInput.sendKeys(username);
    }

    public void submit() {
        submitButton.click();
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-username";
    }
}
