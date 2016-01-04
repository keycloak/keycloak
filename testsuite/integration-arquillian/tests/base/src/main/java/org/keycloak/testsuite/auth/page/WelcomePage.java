package org.keycloak.testsuite.auth.page;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class WelcomePage extends AuthServer {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "passwordConfirmation")
    private WebElement passwordConfirmationInput;

    @FindBy(id = "create-button")
    private WebElement createButton;

    public boolean isPasswordSet() {
        return !driver.getPageSource().contains("Please create an initial admin user to get started.");
    }

    public void setPassword(String username, String password) {
        usernameInput.clear();
        usernameInput.sendKeys(username);

        passwordInput.clear();
        passwordInput.sendKeys(password);

        passwordConfirmationInput.clear();
        passwordConfirmationInput.sendKeys(password);

        createButton.click();

        if (!driver.getPageSource().contains("User created")) {
            throw new RuntimeException("Failed to updated password");
        }
    }

}
