package org.keycloak.testsuite.console.page.authentication;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author tkyjovsk
 * @author mhajas
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class RequiredActions extends Authentication {

    public final static String DEFAULT = "_default";
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
        WebElement checkbox = requiredActionTable.findElement(By.id(id));

        if (checkbox.isSelected() != value) {
            checkbox.click();
        }
    }

    private void setRequiredActionDefaultValue(String id, boolean value) {
        setRequiredActionValue(id + DEFAULT, value);
    }

    public void setTermsAndConditionEnabled(boolean value) {
        setRequiredActionValue(TERMS_AND_CONDITIONS, value);
    }

    public void setTermsAndConditionDefaultAction(boolean value) {
        setRequiredActionDefaultValue(TERMS_AND_CONDITIONS, value);
    }

    public void setVerifyEmailEnabled(boolean value) {
        setRequiredActionValue(VERIFY_EMAIL, value);
    }

    public void setVerifyEmailDefaultAction(boolean value) {
        setRequiredActionDefaultValue(VERIFY_EMAIL, value);
    }

    public void setUpdatePasswordEnabled(boolean value) {
        setRequiredActionValue(UPDATE_PASSWORD, value);
    }

    public void setUpdatePasswordDefaultAction(boolean value) {
        setRequiredActionDefaultValue(UPDATE_PASSWORD, value);
    }

    public void setConfigureTotpEnabled(boolean value) {
        setRequiredActionValue(CONFIGURE_TOTP, value);
    }

    public void setConfigureTotpDefaultAction(boolean value) {
        setRequiredActionDefaultValue(CONFIGURE_TOTP, value);
    }

    public void setUpdateProfileEnabled(boolean value) {
        setRequiredActionValue(UPDATE_PROFILE, value);
    }

    public void setUpdateProfileDefaultAction(boolean value) {
        setRequiredActionDefaultValue(UPDATE_PROFILE, value);
    }
}
