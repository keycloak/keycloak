package org.keycloak.testsuite.auth.page.login;

import java.util.concurrent.TimeUnit;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.page.Form;
import static org.keycloak.testsuite.util.SeleniumUtils.waitGuiForElementNotPresent;
import static org.keycloak.testsuite.util.SeleniumUtils.waitGuiForElementPresent;
import static org.keycloak.testsuite.util.Users.getPasswordCredentialValueOf;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class LoginForm extends Form {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(linkText = "Register")
    private WebElement registerLink;

    @FindBy(name = "login")
    private WebElement loginButton;
    @FindBy(name = "cancel")
    private WebElement cancelButton;

    public WebElement getUsernameInput() {
        return usernameInput;
    }

    public void setUsername(String username) {
        setInputText(usernameInput, username);
    }

    public void setPassword(String password) {
        setInputText(passwordInput, password);
    }

    public void login(UserRepresentation user) {
        login(user.getUsername(), getPasswordCredentialValueOf(user));
    }

    public void login(String username, String password) {
        setUsername(username);
        setPassword(password);
        login();
    }

    public void register() {
        waitGuiForElementPresent(usernameInput, "Login form should be visible");
        registerLink.click();
    }

    public void login() {
        loginButton.click();
    }

    @Override
    public void cancel() {
        cancelButton.click();
    }

    public void waitForRegistrationLinkPresent() {
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        waitGuiForElementPresent(registerLink, 1);
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
    }

    public void waitForRegistrationLinkNotPresent() {
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        waitGuiForElementNotPresent(registerLink, 1);
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
    }

}
