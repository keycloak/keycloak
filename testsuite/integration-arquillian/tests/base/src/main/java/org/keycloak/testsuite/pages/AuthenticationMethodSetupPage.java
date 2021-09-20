package org.keycloak.testsuite.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Setup page for various authentication methods(Password/OTP/RecoveryAuthnCodes etc.)
 *
 * @author <a href="mailto:vnukala@redhat.com">Venkata Nukala</a>
 */
public class AuthenticationMethodSetupPage extends LanguageComboboxAwarePage {

    @FindBy(id = "basic-authentication-categ-title")
    private WebElement basicAuthCategoryTitle;

    @FindBy(id = "two-factor-categ-title")
    private WebElement twoFactorCategoryTitle;

    @FindBy(css = "[id^=recovery-authn-codes-remove]")
    private WebElement removeRecoveryAuthnCodesLink;

    @FindBy(xpath = "//*[@id='modal-confirm']")
    private WebElement confirmBtn;

    @FindBy(xpath = "//*[@id='modal-cancel']")
    private WebElement cancelBtn;

    public void clickRemoveBackupCodesLink() {
        removeRecoveryAuthnCodesLink.click();
    }

    public void clickConfirmButton() {
        confirmBtn.click();
    }

    @Override
    public boolean isCurrent() {

        // Check the backup code text box and label available
        try {
            driver.findElement(By.id("basic-authentication-categ-title"));
            driver.findElement(By.id("two-factor-categ-title"));
        } catch (NoSuchElementException nfe) {
            return false;
        }
        return true;
    }

    @Override
    public void open() throws Exception {
        throw new UnsupportedOperationException();
    }
}
