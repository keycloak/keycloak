package org.keycloak.testsuite.console.page.fragment;

import static org.keycloak.testsuite.util.WaitUtils.waitAjaxForElement;
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

    public void ok() {
        waitAjaxForElement(okButton);
        okButton.click();
    }
    
    public void confirmDeletion() {
        waitAjaxForElement(deleteButton);
        deleteButton.click();
    }

    public void cancel() {
        waitAjaxForElement(cancelButton);
        cancelButton.click();
    }

}
