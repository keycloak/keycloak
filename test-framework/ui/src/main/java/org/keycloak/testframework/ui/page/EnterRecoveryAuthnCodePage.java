package org.keycloak.testframework.ui.page;


import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Signing In Page with required action "Enter Backup Code for authentication"
 *
 * @author <a href="mailto:vnukala@redhat.com">Venkata Nukala</a>
 */
public class EnterRecoveryAuthnCodePage extends AbstractLoginPage {

    @FindBy(xpath = "//label[@for='recoveryCodeInput']")
    private WebElement recoveryAuthnCodeLabel;

    @FindBy(id = "recoveryCodeInput")
    private WebElement recoveryAuthnCodeTextField;

    @FindBy(id = "kc-login")
    private WebElement signInButton;

    public EnterRecoveryAuthnCodePage(ManagedWebDriver driver) {
        super(driver);
    }

    public int getRecoveryAuthnCodeToEnterNumber() {
        String [] recoveryAuthnCodeLabelParts = recoveryAuthnCodeLabel.getText().split("#");
        return Integer.parseInt(recoveryAuthnCodeLabelParts[1]) - 1; // Recovery Authn Code 1 is at element 0 in the list
    }

    public void enterRecoveryAuthnCode(String recoveryCode) {
        recoveryAuthnCodeTextField.sendKeys(recoveryCode);
    }

    public void clickSignInButton() {
        signInButton.click();
    }

    public void waitUntilReloaded() {
        driver.waiting().until((WebDriver d) -> {
            try {
                return recoveryAuthnCodeTextField.getAttribute("value").isEmpty() && !getFeedbackText().isEmpty();
            } catch (StaleElementReferenceException | NoSuchElementException expected) {
                return false;
            }
        });
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-recovery-authn-code-input";
    }

    public String getFeedbackText() {
        return driver.findElement(By.className("kc-feedback-text")).getText().trim();
    }
}
