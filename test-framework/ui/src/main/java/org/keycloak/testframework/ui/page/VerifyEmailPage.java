package org.keycloak.testframework.ui.page;


import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class VerifyEmailPage extends AbstractLoginPage {

    @FindBy(linkText = "Click here")
    private WebElement resendEmailLink;

    @FindBy(name = "cancel-aia")
    private WebElement cancelAIAButton;

    @FindBy(className = "kc-feedback-text")
    private WebElement feedbackText;

    public VerifyEmailPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void clickResendEmail() {
        resendEmailLink.click();
    }

    public String getResendEmailLink() {
        return resendEmailLink.getAttribute("href");
    }

    public String getFeedbackText() {
        return feedbackText.getText();
    }

    public void cancel() {
        cancelAIAButton.click();
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-verify-email";
    }
}
