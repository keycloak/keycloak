package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class IdpLinkEmailPage extends AbstractLoginPage {

    @FindBy(id = "instruction1")
    private WebElement message;

    @FindBy(xpath = "//p[@id='instruction2']/a[text() = 'Click here']")
    private WebElement resendEmailLink;

    @FindBy(xpath = "//p[@id='instruction3']/a[text() = 'Click here']")
    private WebElement continueLink;

    public IdpLinkEmailPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-idp-link-email";
    }

    public String getMessage() {
        return message.getText();
    }

    public void resendEmail() {
        resendEmailLink.click();
    }

    public void continueLink() {
        continueLink.click();
    }
}
