package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class DeleteCredentialPage extends AbstractLoginPage {

    @FindBy(id = "kc-accept")
    private WebElement submitButton;

    @FindBy(id = "kc-decline")
    private WebElement cancelButton;

    @FindBy(id = "kc-delete-text")
    private WebElement message;

    public DeleteCredentialPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-delete-credential";
    }

    public void confirm() {
        submitButton.click();
    }

    public void cancel() {
        cancelButton.click();
    }

    public void assertCredentialInMessage(String expectedLabel) {
        Assertions.assertEquals("Do you want to delete " + expectedLabel + "?", message.getText());
    }
}
