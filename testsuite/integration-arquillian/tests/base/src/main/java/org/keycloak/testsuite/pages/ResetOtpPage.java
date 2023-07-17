package org.keycloak.testsuite.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ResetOtpPage extends AbstractPage {

    @FindBy(id = "kc-otp-reset-form-submit")
    protected WebElement submitButton;

    @FindBy(id = "kc-otp-reset-form-description")
    protected WebElement description;

    @Override
    public boolean isCurrent() {
        return description.getText().equals("Which OTP configuration should be removed?");
    }

    @Override
    public void open() throws Exception {
        // This page is part of the reset credentials flow, so you shouldn't be able to open it by itself.
    }

    public void selectOtp(int index) {
        driver.findElement(By.id("kc-otp-credential-" + index)).click();
    }

    public void submitOtpReset() {
        submitButton.click();
    }
}
