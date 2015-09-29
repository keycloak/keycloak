package org.keycloak.testsuite.console.page.authentication;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author tkyjovsk
 * @author mhajas
 */
public class RequiredActions extends Authentication {

    public final static String ENABLED = "enabled";
    public final static String DEFAULT_ACTION = "defaultAction";

    @FindBy(tagName = "table")
    private WebElement requiredActionTable;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/required-actions";
    }

    private void setRequiredActionValue(String row, String column, boolean value) {
        WebElement checkbox = requiredActionTable.findElement(By.xpath("//td[text()='" + row + "']/..//input[@ng-model='requiredAction." + column + "']"));

        if (checkbox.isSelected() != value) {
            checkbox.click();
        }
    }

    public void setTermsAndConditionEnabled(boolean value) {
        setRequiredActionValue("Terms and Conditions", ENABLED, value);
    }

    public void setTermsAndConditionDefaultAction(boolean value) {
        setRequiredActionValue("Terms and Conditions", DEFAULT_ACTION, value);
    }

    public void setVerifyEmailEnabled(boolean value) {
        setRequiredActionValue("Verify Email", ENABLED, value);
    }

    public void setVerifyEmailDefaultAction(boolean value) {
        setRequiredActionValue("Verify Email", DEFAULT_ACTION, value);
    }

    public void setUpdatePasswordEnabled(boolean value) {
        setRequiredActionValue("Update Password", ENABLED, value);
    }

    public void setUpdatePasswordDefaultAction(boolean value) {
        setRequiredActionValue("Update Password", DEFAULT_ACTION, value);
    }

    public void setConfigureTotpEnabled(boolean value) {
        setRequiredActionValue("Configure Totp", ENABLED, value);
    }

    public void setConfigureTotpDefaultAction(boolean value) {
        setRequiredActionValue("Configure Totp", DEFAULT_ACTION, value);
    }

    public void setUpdateProfileEnabled(boolean value) {
        setRequiredActionValue("Update Profile", ENABLED, value);
    }

    public void setUpdateProfileDefaultAction(boolean value) {
        setRequiredActionValue("Update Profile", DEFAULT_ACTION, value);
    }
}
