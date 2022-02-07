package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * login page for UsernameForm. It contains only username, but not password
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LoginUsernameOnlyPage extends LoginPage {

    @FindBy(id = "input-error-username")
    private WebElement usernameError;

    @Override
    public void login(String username) {
        usernameInput.clear();
        usernameInput.sendKeys(username);

        submitButton.click();
    }

    public String getUsernameError() {
        try {
            return UIUtils.getTextFromElement(usernameError);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    // Click button without fill anything
    public void clickSubmitButton() {
        submitButton.click();
    }

    /**
     * Not supported for this implementation
     *
     * @return
     */
    @Deprecated
    @Override
    public void login(String username, String password) {
        throw new UnsupportedOperationException("Not supported - password field not available");
    }


    /**
     * Not supported for this implementation
     * @return
     */
    @Deprecated
    @Override
    public String getPassword() {
        throw new UnsupportedOperationException("Not supported - password field not available");
    }


    /**
     * Not supported for this implementation
     * @return
     */
    @Deprecated
    @Override
    public void missingPassword(String username) {
        throw new UnsupportedOperationException("Not supported - password field not available");
    }


    @Override
    public boolean isCurrent(String realm) {
        if (!super.isCurrent(realm)) {
            return false;
        }

        // Check there is username field
        try {
            driver.findElement(By.id("username"));
        } catch (NoSuchElementException nfe) {
            return false;
        }

        // Check there is NO password field
        try {
            driver.findElement(By.id("password"));
            return false;
        } catch (NoSuchElementException nfe) {
            // Expected
        }

        return true;
    }
}
