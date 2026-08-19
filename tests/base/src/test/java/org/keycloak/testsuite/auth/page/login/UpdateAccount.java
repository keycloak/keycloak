package org.keycloak.testsuite.auth.page.login;

import org.keycloak.testframework.ui.page.AbstractLoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.auth.page.AccountFields;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class UpdateAccount extends AbstractLoginPage {

    private final AccountFields accountFields;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    public UpdateAccount(ManagedWebDriver driver) {
        super(driver);
        this.accountFields = new AccountFields(driver);
    }

    public AccountFields fields() {
        return accountFields;
    }

    public void submit() {
        submitButton.click();
    }

    @Override
    public String getExpectedPageId() {
        return "login-idp-review-user-profile";
    }
}
