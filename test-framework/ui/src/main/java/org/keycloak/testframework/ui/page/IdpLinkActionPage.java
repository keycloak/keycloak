package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Confirmation page of the {@code idp_link} application-initiated action ("Do you want to link your account with ...?").
 */
public class IdpLinkActionPage extends AbstractLoginPage {

    @FindBy(id = "kc-continue")
    private WebElement continueButton;

    @FindBy(id = "kc-cancel")
    private WebElement cancelButton;

    public IdpLinkActionPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-link-idp-action";
    }

    public void confirm() {
        continueButton.click();
    }

    public void cancel() {
        cancelButton.click();
    }
}
