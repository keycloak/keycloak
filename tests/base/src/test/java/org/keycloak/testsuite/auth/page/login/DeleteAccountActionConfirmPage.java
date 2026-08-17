package org.keycloak.testsuite.auth.page.login;

import org.keycloak.authentication.requiredactions.DeleteAccount;
import org.keycloak.testframework.ui.page.AbstractLoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class DeleteAccountActionConfirmPage extends AbstractLoginPage {

    @FindBy(css = "[name='cancel-aia']")
    private WebElement cancelActionButton;

    @FindBy(css = "[type='submit']")
    private WebElement confirmActionButton;

    public DeleteAccountActionConfirmPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-actions";
    }

    public boolean isCurrent() {
        return driver.getCurrentUrl().contains("login-actions/required-action")
                && driver.getCurrentUrl().contains("execution=" + DeleteAccount.PROVIDER_ID);
    }

    public void clickCancelAIA() {
        cancelActionButton.click();
    }

    public void clickConfirmAction() {
        confirmActionButton.click();
    }

    public String getErrorMessageText() {
        return driver.findElement(By.cssSelector("#kc-content-wrapper > div > span.kc-feedback-text")).getText();
    }
}
