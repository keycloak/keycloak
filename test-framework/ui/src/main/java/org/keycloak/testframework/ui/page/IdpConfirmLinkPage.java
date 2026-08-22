package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class IdpConfirmLinkPage extends AbstractLoginPage {

    @FindBy(id = "updateProfile")
    private WebElement updateProfileButton;

    @FindBy(id = "linkAccount")
    private WebElement linkAccountButton;

    @FindBy(id = "kc-page-title")
    private WebElement message;

    public IdpConfirmLinkPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void clickReviewProfile() {
        updateProfileButton.click();
    }

    public void clickLinkAccount() {
        linkAccountButton.click();
    }

    public String getMessage() {
        return message.getText();
    }

    public boolean isReviewProfileDisplayed() {
        try {
            return updateProfileButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-idp-link-confirm";
    }
}
