package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class IdpConfirmLinkPage extends AbstractLoginPage {

    @FindBy(id = "linkAccount")
    private WebElement linkAccountButton;

    public IdpConfirmLinkPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-idp-link-confirm";
    }

    public void clickLinkAccount() {
        linkAccountButton.click();
    }
}
