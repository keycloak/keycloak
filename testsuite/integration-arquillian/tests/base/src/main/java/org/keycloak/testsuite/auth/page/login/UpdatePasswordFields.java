package org.keycloak.testsuite.auth.page.login;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class UpdatePasswordFields {

    @FindBy(id = "password-new")
    private WebElement newPasswordInput;
    @FindBy(id = "password-confirm")
    private WebElement confirmInput;

    public void setNewPassword(String newPassword) {
        newPasswordInput.clear();
        newPasswordInput.sendKeys(newPassword);
    }

    public void setConfirmPassword(String confirmPassword) {
        confirmInput.clear();
        confirmInput.sendKeys(confirmPassword);
    }
    
    public void setPasswords(String newPassword, String confirmPassword) {
        setNewPassword(newPassword);
        setConfirmPassword(confirmPassword);
    }

}
