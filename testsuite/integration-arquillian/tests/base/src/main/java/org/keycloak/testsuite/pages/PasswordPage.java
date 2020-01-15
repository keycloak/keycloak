package org.keycloak.testsuite.pages;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.OAuthClient;
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

    @FindBy(name = "login")
    private WebElement submitButton;

    @FindBy(className = "alert-error")
    private WebElement loginErrorMessage;

    @FindBy(linkText = "Forgot Password?")
    private WebElement resetPasswordLink;


    public void login(String password) {
        passwordInput.clear();
        passwordInput.sendKeys(password);

        submitButton.click();
    }

    public void clickResetPassword() {
        resetPasswordLink.click();
    }

    public String getPassword() {
        return passwordInput.getAttribute("value");
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
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


    @Override
    public void open() throws Exception {
        throw new UnsupportedOperationException();
    }
}
