package org.keycloak.testsuite.auth.page.login;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.page.Form;
import static org.keycloak.testsuite.admin.Users.getPasswordCredentialValueOf;
import org.keycloak.testsuite.auth.page.account.AccountFields;
import org.keycloak.testsuite.auth.page.account.PasswordFields;
import static org.keycloak.testsuite.util.SeleniumUtils.waitAjaxForElement;
import static org.keycloak.testsuite.util.SeleniumUtils.waitAjaxForElementNotPresent;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class LoginForm extends Form {

    @Page
    private AccountFields accountFields;
    @Page
    private PasswordFields passwordFields;

    @FindBy(name = "login")
    private WebElement loginButton;
    @FindBy(name = "cancel")
    private WebElement cancelButton;

    @FindBy(linkText = "Register")
    private WebElement registerLink;

    public void setUsername(String username) {
        accountFields.setUsername(username);
    }

    public void setPassword(String password) {
        passwordFields.setPassword(password);
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
        waitForUsernameInputPresent();
        waitForRegisterLinkPresent();
        registerLink.click();
    }

    public void login() {
        waitAjaxForElement(loginButton);
        loginButton.click();
    }

    @Override
    public void cancel() {
        waitAjaxForElement(cancelButton);
        cancelButton.click();
    }
    
    public void waitForUsernameInputPresent() {
        accountFields.waitForUsernameInputPresent();
    }

    public void waitForRegisterLinkPresent() {
        waitAjaxForElement(registerLink);
    }

    public void waitForRegistrationLinkNotPresent() {
        waitAjaxForElementNotPresent(registerLink);
    }

}
