package org.keycloak.testsuite.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SetupRecoveryAuthnCodesPage extends LanguageComboboxAwarePage {

    @FindBy(id = "kc-recovery-codes-list")
    private WebElement recoveryAuthnCodesList;

    @FindBy(id = "saveRecoveryAuthnCodesBtn")
    private WebElement saveRecoveryAuthnCodesButton;

    @FindBy(id="kcRecoveryCodesConfirmationCheck")
    private WebElement kcRecoveryCodesConfirmationCheck;

    public void clickSaveRecoveryAuthnCodesButton() {
        kcRecoveryCodesConfirmationCheck.click();
        saveRecoveryAuthnCodesButton.click();
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

    @Override
    public void open() throws Exception {
        throw new UnsupportedOperationException();
    }
}
