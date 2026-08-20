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

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement message;

    public IdpConfirmLinkPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-idp-link-confirm";
    }

    public String getMessage() {
        return message.getText();
    }

    public void clickReviewProfile() {
        updateProfileButton.click();
    }

    public boolean isReviewProfileDisplayed() {
        try {
            return updateProfileButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public void clickLinkAccount() {
        linkAccountButton.click();
    }
}
