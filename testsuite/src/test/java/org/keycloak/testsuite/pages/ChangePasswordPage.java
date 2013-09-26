package org.keycloak.testsuite.pages;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.keycloak.testsuite.Constants;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ChangePasswordPage {

    private static String PATH = Constants.AUTH_SERVER_ROOT + "/rest/realms/demo/account/password";

    @Drone
    private WebDriver browser;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-new")
    private WebElement newPasswordInput;

    @FindBy(id = "password-confirm")
    private WebElement passwordConfirmInput;

    @FindBy(css = "button[type=\"submit\"].primary")
    private WebElement submitButton;

    public void changePassword(String password, String newPassword, String passwordConfirm) {
        passwordInput.sendKeys(password);
        newPasswordInput.sendKeys(newPassword);
        passwordConfirmInput.sendKeys(passwordConfirm);

        submitButton.click();
    }

    public boolean isCurrent() {
        return browser.getPageSource().contains("Change Password");
    }

    public void open() {
        browser.navigate().to(PATH);
    }

}
