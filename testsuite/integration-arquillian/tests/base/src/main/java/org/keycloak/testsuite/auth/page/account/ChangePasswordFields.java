package org.keycloak.testsuite.auth.page.account;

import org.keycloak.testsuite.auth.page.login.UpdatePasswordFields;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class ChangePasswordFields extends UpdatePasswordFields {

    @FindBy(id = "password")
    private WebElement oldPasswordInput;

    public void setOldPassword(String oldPassword) {
        oldPasswordInput.clear();
        oldPasswordInput.sendKeys(oldPassword);
    }

    public void setPasswords(String oldPassword, String newPassword, String confirmPassword) {
        setOldPassword(oldPassword);
        setPasswords(newPassword, confirmPassword);
    }

}
