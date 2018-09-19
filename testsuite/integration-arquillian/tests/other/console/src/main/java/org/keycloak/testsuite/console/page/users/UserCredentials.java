package org.keycloak.testsuite.console.page.users;

import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.setTextInputValue;

/**
 *
 * @author tkyjovsk
 */
public class UserCredentials extends User {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/user-credentials";
    }

    @FindBy(id = "newPas")
    private WebElement newPasswordInput;

    @FindBy(id = "confirmPas")
    private WebElement confirmPasswordInput;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='temporaryPassword']]")
    private OnOffSwitch temporaryOnOffSwitch;

    @FindBy(xpath = ".//div[not(contains(@class, 'ng-hide'))]/button[contains(@data-ng-click, 'resetPassword')]")
    private WebElement resetPasswordButton;

    public void setNewPassword(String newPassword) {
        setTextInputValue(newPasswordInput, newPassword);
    }

    public void setConfirmPassword(String confirmPassword) {
        setTextInputValue(confirmPasswordInput, confirmPassword);
    }

    public void setTemporary(boolean temporary) {
        temporaryOnOffSwitch.setOn(temporary);
    }

    public void clickResetPasswordAndConfirm() {
        resetPasswordButton.click();
        modalDialog.ok();
    }
    
    public void resetPassword(String newPassword) {
        resetPassword(newPassword, newPassword);
    }
    public void resetPassword(String newPassword, String confirmPassword) {
        setNewPassword(newPassword);
        setConfirmPassword(confirmPassword);
        clickResetPasswordAndConfirm();
    }
    
}
