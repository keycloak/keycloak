package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.rule.WebResource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginPasswordResetPage extends Page {

    @WebResource
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
        throw new UnsupportedOperationException();
    }

}
