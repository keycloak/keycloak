package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.UIUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class PhoneOtpLoginPage extends LanguageComboboxAwarePage {

    @FindBy(id = "phoneNumber")
    private WebElement phoneNumberInput;

    @FindBy(id = "kc-login")
    private WebElement continueButton;

    @FindBy(id = "input-error-phone-number")
    private WebElement phoneNumberError;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement alertMessage;

    public void submitPhoneNumber(String phoneNumber) {
        UIUtils.setTextInputValue(phoneNumberInput, phoneNumber);
        UIUtils.clickLink(continueButton);
    }

    public String getInputError() {
        try {
            return UIUtils.getTextFromElement(phoneNumberError);
        } catch (NoSuchElementException e) {
            return null;
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
            driver.findElement(By.id("kc-phone-otp-login-form"));
            driver.findElement(By.id("phoneNumber"));
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
