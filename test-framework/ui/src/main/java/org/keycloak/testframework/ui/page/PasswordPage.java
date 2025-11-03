package org.keycloak.testframework.ui.page;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * login page for PasswordForm. It contains only password, but not username
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PasswordPage extends AbstractPage {

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

    public PasswordPage(WebDriver driver) {
        super(driver);
    }

    public void login(String password) {
        passwordInput.clear();
        passwordInput.sendKeys(password);

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
        return "password";
    }
}
