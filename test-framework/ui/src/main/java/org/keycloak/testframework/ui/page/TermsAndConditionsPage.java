package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class TermsAndConditionsPage extends AbstractLoginPage {

    @FindBy(id = "kc-accept")
    private WebElement submitButton;

    @FindBy(id = "kc-decline")
    private WebElement cancelButton;

    public TermsAndConditionsPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void acceptTerms() {
        submitButton.click();
    }
    public void declineTerms() {
        cancelButton.click();
    }

    @Override
    public String getExpectedPageId() {
        return "login-terms";
    }
}
