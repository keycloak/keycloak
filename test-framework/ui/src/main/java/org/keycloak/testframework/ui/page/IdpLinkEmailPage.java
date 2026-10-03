package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class IdpLinkEmailPage extends AbstractPage {

    @FindBy(xpath = "//p[@id='instruction3']/a[text() = 'Click here']")
    private WebElement continueLink;

    public IdpLinkEmailPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-idp-link-email";
    }

    public void continueLink() {
        continueLink.click();
    }
}
