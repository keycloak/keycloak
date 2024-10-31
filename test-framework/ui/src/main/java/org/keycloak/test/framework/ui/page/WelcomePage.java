package org.keycloak.test.framework.ui.page;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class WelcomePage extends AbstractPage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-confirmation")
    private WebElement passwordConfirmationInput;

    @FindBy(css = "[type=submit]")
    private WebElement submitButton;

    @FindBy(css = ".pf-v5-c-alert")
    private WebElement pageAlert;

    @FindBy(css = ".pf-v5-c-title")
    private WebElement welcomeMessage;

    public WelcomePage(WebDriver driver) {
        super(driver);
    }

    public void navigateTo() {
        driver.get("http://localhost:8080");
    }

    public void fillRegistration(String username, String password) {
        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
        passwordConfirmationInput.sendKeys(password);
    }

    public void login(String username, String password) {
        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
        submitButton.click();
    }

    public void submit() {
        submitButton.click();
    }

    public void assertUserCreated() {
        Assertions.assertTrue(pageAlert.getText().contains("User created"));
    }

    public boolean isPasswordSet() {
        return !(driver.getPageSource().contains("Create a temporary administrative user") ||
                driver.getPageSource().contains("You will need local access to create the temporary administrative user.") ||
                driver.getPageSource().contains("you first create a temporary administrative user. Later, to harden security, create a new permanent administrative user"));
    }

    public void navigateToAdminConsole() {
        driver.get("http://localhost:8080/admin/master/console");
    }

    public String getWelcomeMessage() {
        return welcomeMessage.getText();
    }
}
