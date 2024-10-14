package org.keycloak.test.framework.page;

import org.junit.jupiter.api.Assertions;
import org.keycloak.test.framework.annotations.InjectOAuthClient;
import org.keycloak.test.framework.oauth.OAuthClient;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginPage extends AbstractPage {

    @InjectOAuthClient
    protected OAuthClient oAuthClient;

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(css = "[type=submit]")
    private WebElement submitButton;

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void open() {
        driver.navigate().to(oAuthClient.getRealmUrl());
        assertCurrent();
    }

    @Override
    public boolean isCurrent() {
        return isCurrent("test");
    }

    public void fillLogin(String username, String password) {
        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
    }

    public void loginUsername(String username) {
        usernameInput.sendKeys(username);
        submitButton.click();
    }

    public void submit() {
        submitButton.click();
    }

    public boolean isCurrent(String realm) {
        return driver.getTitle().equals("Sign in to " + realm) || driver.getTitle().equals("Anmeldung bei " + realm);
    }

    public void assertCurrent(String realm) {
        String name = getClass().getSimpleName();
        Assertions.assertTrue(isCurrent(realm), "Expected " + name + " but was " + driver.getTitle() + " (" + driver.getCurrentUrl() + ")");
    }



}
