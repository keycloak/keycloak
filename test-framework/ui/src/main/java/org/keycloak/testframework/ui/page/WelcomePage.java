package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class WelcomePage extends AbstractPage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-confirmation")
    private WebElement passwordConfirmationInput;

    @FindBy(xpath = "//button")
    private WebElement submitButton;

    @FindBy(css = ".pf-v5-c-alert")
    private WebElement pageAlert;

    @FindBy(css = ".pf-v5-c-title")
    private WebElement welcomeMessage;

    @FindBy(css = ".pf-v5-c-login__main-header-desc")
    private WebElement welcomeDescription;

    @FindBy(css = ".pf-v5-c-button")
    private WebElement openAdminConsoleLink;

    public WelcomePage(ManagedWebDriver driver) {
        super(driver);
    }

    public void fillRegistration(String username, String password) {
        fillRegistration(username, null, null, null, password);
    }

    public void fillRegistration(String username, String firstName, String lastName, String email, String password) {
        usernameInput.sendKeys(username);
        if (firstName != null) {
            firstNameInput.sendKeys(firstName);
        }
        if (lastName != null) {
            lastNameInput.sendKeys(lastName);
        }
        if (email != null) {
            emailInput.sendKeys(email);
        }
        passwordInput.sendKeys(password);
        passwordConfirmationInput.sendKeys(password);
    }

    public void submit() {
        submitButton.click();
    }

    public void clickOpenAdminConsole() {
        openAdminConsoleLink.click();
    }

    public String getWelcomeMessage() {
        return welcomeMessage.getText();
    }

    public String getWelcomeDescription() {
        return welcomeDescription.getText();
    }

    public String getPageAlert() {
        return pageAlert.getText();
    }

    @Override
    public String getExpectedPageId() {
        return "welcome";
    }
}
