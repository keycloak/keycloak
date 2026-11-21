package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class IdpConfirmOverrideLinkPage extends AbstractLoginPage {

    @FindBy(id = "confirmOverride")
    private WebElement confirmOverrideButton;

    public IdpConfirmOverrideLinkPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-idp-link-confirm-override";
    }

    public void clickConfirmOverride() {
        confirmOverrideButton.click();
    }
}
