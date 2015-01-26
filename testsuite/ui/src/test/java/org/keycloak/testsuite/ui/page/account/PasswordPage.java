package org.keycloak.testsuite.ui.page.account;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class PasswordPage {

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-new")
    private WebElement newPasswordInput;

    @FindBy(id = "password-confirm")
    private WebElement confirmInput;

    @FindByJQuery("button[value='Save']")
    private WebElement save;

    public void setPassword(String oldPassword, String newPassword) {
        passwordInput.clear();
        passwordInput.sendKeys(oldPassword);
        newPasswordInput.clear();
        newPasswordInput.sendKeys(newPassword);
        confirmInput.clear();
        confirmInput.sendKeys(newPassword);
    }

    public void setOldPasswordField(String oldPassword) {
        passwordInput.clear();
        passwordInput.sendKeys(oldPassword);
    }

    public void setNewPasswordField(String newPassword) {
        newPasswordInput.clear();
        newPasswordInput.sendKeys(newPassword);
    }

    public void setConfirmField(String confirmPassword) {
        confirmInput.clear();
        confirmInput.sendKeys(confirmPassword);
    }

    public void save() {
        save.click();
    }
}
