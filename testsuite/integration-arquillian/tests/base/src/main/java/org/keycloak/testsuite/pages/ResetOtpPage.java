package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.UIUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ResetOtpPage extends AbstractPage {

    @FindBy(id = "kc-otp-reset-form-submit")
    protected WebElement submitButton;

    @FindBy(id = "kc-otp-reset-form-description")
    protected WebElement description;

    @Override
    public String getExpectedPageId() {
        return "login-login-reset-otp";
    }

    public void selectOtp(int index) {
        driver.findElement(By.id("kc-otp-credential-" + index)).click();
    }

    public void submitOtpReset() {
        UIUtils.clickLink(submitButton);
    }
}
