package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * login page for PasswordForm. It contains only password, but not username
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PasswordPage extends AbstractLoginPage {

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "input-error-password")
    private WebElement passwordError;

    @FindBy(name = "login")
    private WebElement submitButton;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement loginErrorMessage;

    @FindBy(linkText = "Forgot Password?")
    private WebElement resetPasswordLink;

    @FindBy(id = "try-another-way")
    private WebElement tryAnotherWayLink;

    public PasswordPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void fillPassword(String password) {
        passwordInput.clear();
        passwordInput.sendKeys(password);
    }

    public void submit() {
        submitButton.click();
    }

    public String getPassword() {
        return passwordInput.getAttribute("value");
    }

    public String getError() {
        try {
            return loginErrorMessage.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public void clickTryAnotherWayLink() {
        tryAnotherWayLink.click();
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-password";
    }
}
