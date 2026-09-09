package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.UIUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class PhoneOtpCodePage extends LanguageComboboxAwarePage {

    @FindBy(id = "otp")
    private WebElement otpInput;

    @FindBy(id = "kc-login")
    private WebElement submitButton;

    @FindBy(id = "kc-resend")
    private WebElement resendButton;

    @FindBy(id = "input-error-otp-code")
    private WebElement otpError;

    @FindBy(id = "input-error-otp")
    private WebElement otpErrorV2;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement alertMessage;

    public void submitCode(String code) {
        UIUtils.setTextInputValue(otpInput, code);
        UIUtils.clickLink(submitButton);
    }

    public void resendCode() {
        UIUtils.clickLink(resendButton);
    }

    public String getInputError() {
        try {
            return UIUtils.getTextFromElement(otpError);
        } catch (NoSuchElementException e) {
            try {
                return UIUtils.getTextFromElement(otpErrorV2);
            } catch (NoSuchElementException ex) {
                return null;
            }
        }
    }

    public String getAlertError() {
        try {
            return UIUtils.getTextFromElement(alertMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    public boolean isCurrent() {
        try {
            driver.findElement(By.id("kc-phone-otp-code-form"));
            driver.findElement(By.id("otp"));
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
