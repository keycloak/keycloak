package org.keycloak.testsuite.console.page.users;

import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import static org.keycloak.testsuite.page.Form.setInputValue;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import static org.keycloak.testsuite.util.WaitUtils.*;

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

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='temporaryPassword']]")
    private OnOffSwitch temporaryOnOffSwitch;

    @FindBy(xpath = ".//div[not(contains(@class, 'ng-hide'))]/button[contains(@data-ng-click, 'resetPassword')]")
    private WebElement resetPasswordButton;

    public void setNewPassword(String newPassword) {
        setInputValue(newPasswordInput, newPassword);
    }

    public void setConfirmPassword(String confirmPassword) {
        setInputValue(confirmPasswordInput, confirmPassword);
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
