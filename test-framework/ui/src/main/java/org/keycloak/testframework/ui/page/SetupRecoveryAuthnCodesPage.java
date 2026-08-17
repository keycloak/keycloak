package org.keycloak.testframework.ui.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SetupRecoveryAuthnCodesPage extends LogoutSessionsPage {

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
        JavascriptExecutor js = (JavascriptExecutor) driver.driver();
        js.executeScript("document.getElementsByName('generatedRecoveryAuthnCodes')[0].value='" + codes + "'");
    }

    public String getGeneratedAtHidden() {
        return generatedAtHidden.getAttribute("value");
    }

    public void setGeneratedAtHidden(String at) {
        JavascriptExecutor js = (JavascriptExecutor) driver.driver();
        js.executeScript("document.getElementsByName('generatedAt')[0].value='" + at + "'");
    }

    public List<String> getRecoveryAuthnCodes() {
        List<String> result = new ArrayList<>();
        Scanner scanner = new Scanner(recoveryAuthnCodesList.getText());
        while (scanner.hasNextLine()) {
            result.add(scanner.nextLine());
        }
        scanner.close();
        return result;
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-recovery-authn-code-config";
    }
}
