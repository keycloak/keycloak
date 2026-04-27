package org.keycloak.testsuite.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.keycloak.testsuite.util.UIUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
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

    public void clickSaveRecoveryAuthnCodesButton() {
        UIUtils.switchCheckbox(kcRecoveryCodesConfirmationCheck, true);
        UIUtils.clickLink(saveRecoveryAuthnCodesButton);
    }

    public String getGeneratedRecoveryAuthnCodesHidden() {
        return generatedRecoveryAuthnCodesHidden.getAttribute("value");
    }

    public void setGeneratedRecoveryAuthnCodesHidden(String codes) {
        final JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("document.getElementsByName('generatedRecoveryAuthnCodes')[0].value='" + codes + "'");
    }

    public String getGeneratedAtHidden() {
        return generatedAtHidden.getAttribute("value");
    }

    public void setGeneratedAtHidden(String at) {
        final JavascriptExecutor js = (JavascriptExecutor) driver;
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
    public boolean isCurrent() {

        // Check the backup code text box and label available
        try {
            driver.findElement(By.id("kc-recovery-codes-list"));
            driver.findElement(By.id("saveRecoveryAuthnCodesBtn"));
        } catch (NoSuchElementException nfe) {
            return false;
        }
        return true;
    }

}
