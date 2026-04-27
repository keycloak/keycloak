package org.keycloak.tests.forms.page;

import org.keycloak.testframework.ui.page.AbstractPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginConfigTotpPage extends AbstractPage {
    @FindBy(id = "totpSecret")
    private WebElement totpSecret;

    @FindBy(id = "totp")
    private WebElement totpInput;

    @FindBy(id = "userLabel")
    private WebElement totpLabelInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(name = "cancel-aia")
    private WebElement cancelAIAButton;

    @FindBy(id = "mode-barcode")
    private WebElement barcodeLink;

    @FindBy(id = "mode-manual")
    private WebElement manualLink;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement loginAlertErrorMessage;

    @FindBy(id = "input-error-otp-code")
    private WebElement totpInputCodeError;

    @FindBy(id = "input-error-otp-label")
    private WebElement totpInputLabelError;

    public LoginConfigTotpPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-config-totp";
    }

    public void configure(String totp) {
        totpInput.sendKeys(totp);
        submit();
    }

    public void configure(String totp, String userLabel) {
        totpInput.sendKeys(totp);
        totpLabelInput.sendKeys(userLabel);
        submit();
    }

    public void submit() {
        submitButton.click();
    }

    public void cancel() {
        cancelAIAButton.click();
    }

    public String getTotpSecret() {
        return totpSecret.getAttribute("value");
    }

    public void clickManual() {
        manualLink.click();
    }

    public void clickBarcode() {
        barcodeLink.click();
    }

    public String getInputCodeError() {
        try {
            return totpInputCodeError.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getInputLabelError() {
        try {
            return totpInputLabelError.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getAlertError() {
        try {
            return loginAlertErrorMessage.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public boolean isCancelDisplayed() {
        try {
            return cancelAIAButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
