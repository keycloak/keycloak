package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * login page for PasswordForm. It contains only password, but not username
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PasswordPage extends LanguageComboboxAwarePage {

    @ArquillianResource
    protected OAuthClient oauth;

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


    public void login(String password) {
        passwordInput.clear();
        passwordInput.sendKeys(password);

        UIUtils.clickLink(submitButton);
    }

    public void clickResetPassword() {
        UIUtils.clickLink(resetPasswordLink);
    }

    public String getPassword() {
        return passwordInput.getAttribute("value");
    }

    public String getPasswordError() {
        try {
            return UIUtils.getTextFromElement(passwordError);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getError() {
        try {
            return UIUtils.getTextFromElement(loginErrorMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
    }


    public boolean isCurrent() {
        String realm = "test";
        return isCurrent(realm);
    }

    public boolean isCurrent(String realm) {
        // Check there is NO username field
        try {
            driver.findElement(By.id("username"));
            return false;
        } catch (NoSuchElementException nfe) {
            // Expected
        }

        // Check there is password field
        try {
            driver.findElement(By.id("kc-attempted-username"));
            driver.findElement(By.id("password"));
        } catch (NoSuchElementException nfe) {
            return false;
        }

        return true;
    }

}
