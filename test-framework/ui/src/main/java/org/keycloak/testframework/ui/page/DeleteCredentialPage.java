package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Confirmation page of the {@code delete_credential} application-initiated action ("Do you want to delete ...?").
 */
public class DeleteCredentialPage extends AbstractLoginPage {

    @FindBy(id = "kc-accept")
    private WebElement confirmButton;

    @FindBy(id = "kc-decline")
    private WebElement cancelButton;

    public DeleteCredentialPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-delete-credential";
    }

    public void confirm() {
        confirmButton.click();
    }

    public void cancel() {
        cancelButton.click();
    }
}
