package org.keycloak.testsuite.console.page.fragment;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class ModalDialog {

    @FindBy(xpath = ".//button[text()='Cancel']")
    private WebElement cancelButton;
    @FindBy(xpath = ".//button[text()='Delete']")
    private WebElement deleteButton;

    @FindBy(xpath = ".//button[@ng-click='ok()']")
    private WebElement okButton;
    @FindBy(id = "name")
    private WebElement nameInput;

    public void ok() {
        waitUntilElement(okButton).is().present();
        okButton.click();
    }
    
    public void confirmDeletion() {
        waitUntilElement(deleteButton).is().present();
        deleteButton.click();
    }

    public void cancel() {
        waitUntilElement(cancelButton).is().present();
        cancelButton.click();
    }

    public void setName(String name) {
        waitUntilElement(nameInput).is().present();
        nameInput.clear();
        nameInput.sendKeys(name);
    }
}
