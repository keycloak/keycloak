package org.keycloak.testframework.ui.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SetupRecoveryAuthnCodesPage extends AbstractLoginPage {

    @FindBy(id = "kc-recovery-codes-list")
    private WebElement recoveryAuthnCodesList;

    @FindBy(id = "saveRecoveryAuthnCodesBtn")
    private WebElement saveRecoveryAuthnCodesButton;

    @FindBy(id = "kcRecoveryCodesConfirmationCheck")
    private WebElement kcRecoveryCodesConfirmationCheck;

    @FindBy(name = "generatedRecoveryAuthnCodes")
    private WebElement generatedRecoveryAuthnCodesHidden;

    @FindBy(name = "generatedAt")
    private WebElement generatedAtHidden;

    @FindBy(id = "logout-sessions")
    private WebElement logoutSessionsCheckbox;

    public SetupRecoveryAuthnCodesPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void clickSaveRecoveryAuthnCodesButton() {
        if (!kcRecoveryCodesConfirmationCheck.isSelected()) {
            kcRecoveryCodesConfirmationCheck.click();
        }
        saveRecoveryAuthnCodesButton.click();
    }

    public String getGeneratedRecoveryAuthnCodesHidden() {
        return generatedRecoveryAuthnCodesHidden.getAttribute("value");
    }

    public void setGeneratedRecoveryAuthnCodesHidden(String codes) {
        final JavascriptExecutor js = (JavascriptExecutor) driver.driver();
        js.executeScript("document.getElementsByName('generatedRecoveryAuthnCodes')[0].value='" + codes + "'");
    }

    public String getGeneratedAtHidden() {
        return generatedAtHidden.getAttribute("value");
    }

    public void setGeneratedAtHidden(String at) {
        final JavascriptExecutor js = (JavascriptExecutor) driver.driver();
        js.executeScript("document.getElementsByName('generatedAt')[0].value='" + at + "'");
    }

    public List<String> getRecoveryAuthnCodes() {
        String recoveryAuthnCodesText =  recoveryAuthnCodesList.getText();
        List<String> recoveryAuthnCodesList = new ArrayList<>();
        Scanner scanner = new Scanner(recoveryAuthnCodesText);
        while (scanner.hasNextLine()) {
            recoveryAuthnCodesList.add(scanner.nextLine());
        }
        scanner.close();
        return recoveryAuthnCodesList;
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-recovery-authn-code-config";
    }

    public boolean isLogoutSessionsChecked() {
        return logoutSessionsCheckbox.isSelected();
    }

    public void checkLogoutSessions() {
        if (!isLogoutSessionsChecked()) {
            logoutSessionsCheckbox.click();
        }
    }

    public void uncheckLogoutSessions() {
        if (isLogoutSessionsChecked()) {
            logoutSessionsCheckbox.click();
        }
    }
}
