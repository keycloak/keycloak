package org.keycloak.testsuite.console.page.users;

import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class UserCredentials extends User {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/user-credentials";
    }

    @FindBy(id = "password")
    private WebElement newPasswordInput;

    @FindBy(id = "confirmPassword")
    private WebElement confirmPasswordInput;

    @FindBy(xpath = "//div[@class='onoffswitch' and ./input[@id='temporaryPassword']]")
    private OnOffSwitch temporaryOnOffSwitch;

    @FindBy
    private WebElement resetPasswordButton;

    public void setNewPassword(String newPassword) {
        newPasswordInput.clear();
        if (newPassword != null) {
            newPasswordInput.sendKeys(newPassword);
        }
    }

    public void setConfirmPassword(String confirm) {
        confirmPasswordInput.clear();
        if (confirm != null) {
            confirmPasswordInput.sendKeys(confirm);
        }
    }
    
    public void setTemporary(boolean temporary) {
        temporaryOnOffSwitch.setOn(temporary);
    }
    
    public void resetPassword() {
        resetPasswordButton.click();
    }

}
