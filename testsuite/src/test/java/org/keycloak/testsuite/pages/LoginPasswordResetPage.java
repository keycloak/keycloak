package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.rule.Driver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginPasswordResetPage {

    private static String PATH = Constants.AUTH_SERVER_ROOT + "/rest/realms/demo/account/password";

    @Driver
    private WebDriver browser;

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    public void changePassword(String username, String email) {
        usernameInput.sendKeys(username);
        emailInput.sendKeys(email);

        submitButton.click();
    }

    public boolean isCurrent() {
        return browser.getTitle().equals("Reset password");
    }

    public void open() {
        browser.navigate().to(PATH);
    }

}
