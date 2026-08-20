package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class IdpLinkActionPage extends AbstractLoginPage {

    @FindBy(id = "kc-continue")
    private WebElement submitButton;

    @FindBy(id = "kc-cancel")
    private WebElement cancelButton;

    @FindBy(id = "kc-link-text")
    private WebElement message;

    public IdpLinkActionPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-link-idp-action";
    }

    public void confirm() {
        submitButton.click();
    }

    public void cancel() {
        cancelButton.click();
    }

    public void assertIdpInMessage(String expectedIdpDisplayName) {
        Assertions.assertEquals("Do you want to link your account with " + expectedIdpDisplayName + "?", message.getText());
    }
}
