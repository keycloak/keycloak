package org.keycloak.testframework.ui.page;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class TermsAndConditionsPage extends AbstractPage {

    @FindBy(id = "kc-accept")
    private WebElement submitButton;

    @FindBy(id = "kc-decline")
    private WebElement cancelButton;

    public TermsAndConditionsPage(WebDriver driver) {
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
