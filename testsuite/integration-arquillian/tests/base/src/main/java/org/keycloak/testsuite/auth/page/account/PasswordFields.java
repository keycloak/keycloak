package org.keycloak.testsuite.auth.page.account;

import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class PasswordFields extends Form {

    @FindBy(id = "password")
    private WebElement passwordInput;
    @FindBy(id = "password-new")
    private WebElement newPasswordInput;
    @FindBy(id = "password-confirm")
    private WebElement confirmPasswordInput;

    public void setPassword(String password) {
        setInputValue(passwordInput, password);
    }

    public void setNewPassword(String newPassword) {
        setInputValue(newPasswordInput, newPassword);
    }

    public void setConfirmPassword(String confirmPassword) {
        setInputValue(confirmPasswordInput, confirmPassword);
    }

    public void setPasswords(String password, String newPassword, String confirmPassword) {
        setPassword(password);
        setNewPassword(newPassword);
        setConfirmPassword(confirmPassword);
    }

}
