package org.keycloak.testsuite.console.page.authentication;

import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickBtnAndWaitForAlert;

/**
 * @author tkyjovsk
 * @author mhajas
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class RequiredActions extends Authentication {

    public final static String ENABLED = ".enabled";
    public final static String DEFAULT = ".defaultAction";
    public final static String CONFIGURE_TOTP = "CONFIGURE_TOTP";
    public final static String UPDATE_PROFILE = "UPDATE_PROFILE";
    public final static String TERMS_AND_CONDITIONS = "terms_and_conditions";
    public final static String UPDATE_PASSWORD = "UPDATE_PASSWORD";
    public final static String VERIFY_EMAIL = "VERIFY_EMAIL";

    @FindBy(tagName = "table")
    private WebElement requiredActionTable;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/required-actions";
    }

    private void setRequiredActionValue(String id, boolean value) {
        WaitUtils.waitUntilElement(requiredActionTable).is().present();

        WebElement checkbox = requiredActionTable.findElement(By.id(id));

        if (checkbox.isEnabled() && checkbox.isSelected() != value) {
            clickBtnAndWaitForAlert(checkbox);
        }
    }

    private void setRequiredActionEnabledValue(String id, boolean value) {
        setRequiredActionValue(id + ENABLED, value);
    }

    private void setRequiredActionDefaultValue(String id, boolean value) {
        setRequiredActionValue(id + DEFAULT, value);
    }

    public void setTermsAndConditionEnabled(boolean value) {
        setRequiredActionEnabledValue(TERMS_AND_CONDITIONS, value);
    }

    public void setTermsAndConditionDefaultAction(boolean value) {
        setRequiredActionDefaultValue(TERMS_AND_CONDITIONS, value);
    }

    public void setVerifyEmailEnabled(boolean value) {
        setRequiredActionEnabledValue(VERIFY_EMAIL, value);
    }

    public void setVerifyEmailDefaultAction(boolean value) {
        setRequiredActionDefaultValue(VERIFY_EMAIL, value);
    }

    public void setUpdatePasswordEnabled(boolean value) {
        setRequiredActionEnabledValue(UPDATE_PASSWORD, value);
    }

    public void setUpdatePasswordDefaultAction(boolean value) {
        setRequiredActionDefaultValue(UPDATE_PASSWORD, value);
    }

    public void setConfigureTotpEnabled(boolean value) {
        setRequiredActionEnabledValue(CONFIGURE_TOTP, value);
    }

    public void setConfigureTotpDefaultAction(boolean value) {
        setRequiredActionDefaultValue(CONFIGURE_TOTP, value);
    }

    public void setUpdateProfileEnabled(boolean value) {
        setRequiredActionEnabledValue(UPDATE_PROFILE, value);
    }

    public void setUpdateProfileDefaultAction(boolean value) {
        setRequiredActionDefaultValue(UPDATE_PROFILE, value);
    }

    private boolean getRequiredActionValue(String id) {
        WaitUtils.waitUntilElement(requiredActionTable).is().present();

        WebElement checkbox = requiredActionTable.findElement(By.id(id));

        return checkbox.isSelected();
    }

    private boolean getRequiredActionEnabledValue(String id) {
        return getRequiredActionValue(id + ENABLED);
    }

    private boolean getRequiredActionDefaultValue(String id) {
        return getRequiredActionValue(id + DEFAULT);
    }

    public boolean getTermsAndConditionEnabled() {
        return getRequiredActionEnabledValue(TERMS_AND_CONDITIONS);
    }

    public boolean getTermsAndConditionDefaultAction() {
        return getRequiredActionDefaultValue(TERMS_AND_CONDITIONS);
    }

    public boolean getVerifyEmailEnabled() {
        return getRequiredActionEnabledValue(VERIFY_EMAIL);
    }

    public boolean getVerifyEmailDefaultAction() {
        return getRequiredActionDefaultValue(VERIFY_EMAIL);
    }

    public boolean getUpdatePasswordEnabled() {
        return getRequiredActionEnabledValue(UPDATE_PASSWORD);
    }

    public boolean getUpdatePasswordDefaultAction() {
        return getRequiredActionDefaultValue(UPDATE_PASSWORD);
    }

    public boolean getConfigureTotpEnabled() {
        return getRequiredActionEnabledValue(CONFIGURE_TOTP);
    }

    public boolean getConfigureTotpDefaultAction() {
        return getRequiredActionDefaultValue(CONFIGURE_TOTP);
    }

    public boolean getUpdateProfileEnabled() {
        return getRequiredActionEnabledValue(UPDATE_PROFILE);
    }

    public boolean getUpdateProfileDefaultAction() {
        return getRequiredActionDefaultValue(UPDATE_PROFILE);
    }
}
