package org.keycloak.testsuite.console.page.users;

import java.util.List;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.jgroups.util.Util.assertFalse;
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

    @FindBy(xpath = ".//table[contains(@class,'credentials-table')]")
    private WebElement credentialsTable;

    private List<WebElement> credentialsTableRows() {
        return credentialsTable.findElements(By.xpath("./tbody/tr"));
    }

    private List<WebElement> credentialsTableRows(String credentialType) {
        return credentialsTable.findElements(By.xpath(String.format("./tbody/tr[./td[position()=2 and ./*[text()='%s']]]", credentialType)));
    }

    private List<WebElement> credentialsTableRows(String credentialType, String credentialLabel) {
        return credentialsTable.findElement(By.xpath(String.format("./tbody//tr[./td[position()=2 and ./*[text()='%s']] and ./td[position()=3 and ./input[@value='%s']]]", credentialType, credentialLabel)));
    }

    private WebElement rowActionButton(WebElement row, String action) {
        return row.findElement(By.xpath(String.format(
                ".//td[contains(@class, 'credential-action-cell')]/div[contains(@data-ng-click, '%s')]",
                action)));
    }

    public void deletePassword() {
        List<WebElement> passwordRows = credentialsTableRows("password");
        assertFalse("User shouldn't have more than one password credential.", passwordRows.size() > 1);
        log.debug("Deleting password.");
        if (passwordRows.isEmpty()) {
            log.debug("Password credential not found in the credentials table. Skipping deletion.");
        } else {
            rowActionButton(passwordRows.get(0), "deleteCredential").click();
            modalDialog.ok();
        }
    }

    public void setNewPassword(String newPassword) {
        setTextInputValue(newPasswordInput, newPassword);
    }

    public void setConfirmPassword(String confirmPassword) {
        setTextInputValue(confirmPasswordInput, confirmPassword);
    }

    public void setTemporary(boolean temporary) {
        temporaryOnOffSwitch.setOn(temporary);
    }

    public void resetPassword(String newPassword) {
        resetPassword(newPassword, newPassword);
    }

    public void resetPassword(String newPassword, String confirmPassword) {
        setNewPassword(newPassword);
        setConfirmPassword(confirmPassword);
        resetPasswordButton.click();
        modalDialog.ok();
    }

}
