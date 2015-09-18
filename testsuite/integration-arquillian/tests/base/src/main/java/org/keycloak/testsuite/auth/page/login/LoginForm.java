package org.keycloak.testsuite.auth.page.login;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.page.Form;
import static org.keycloak.testsuite.admin.Users.getPasswordOf;
import org.keycloak.testsuite.auth.page.account.AccountFields;
import org.keycloak.testsuite.auth.page.account.PasswordFields;
import static org.keycloak.testsuite.util.WaitUtils.waitAjaxForElement;
import static org.keycloak.testsuite.util.WaitUtils.waitAjaxForElementNotPresent;
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
//    @FindBy(name = "cancel")
//    private WebElement cancelButton;

    @FindBy(linkText = "Register")
    private WebElement registerLink;
    @FindBy(linkText = "Forgot Password?")
    private WebElement forgottenPassword;

    public void setUsername(String username) {
        accountFields.setUsername(username);
    }

    public void setPassword(String password) {
        passwordFields.setPassword(password);
    }

    public void login(UserRepresentation user) {
        login(user.getUsername(), getPasswordOf(user));
    }

    public void login(String username, String password) {
        setUsername(username);
        setPassword(password);
        login();
    }

    public void register() {
        waitForUsernameInputPresent();
        waitAjaxForElement(registerLink);
        registerLink.click();
    }

    public void login() {
        waitAjaxForElement(loginButton);
        loginButton.click();
    }
    
    public void forgotPassword() {
        waitAjaxForElement(forgottenPassword);
        forgottenPassword.click();
    }

//    @Override
//    public void cancel() {
//        waitAjaxForElement(cancelButton);
//        cancelButton.click();
//    }
    
    public void waitForUsernameInputPresent() {
        accountFields.waitForUsernameInputPresent();
    }

    public void waitForRegisterLinkNotPresent() {
        waitAjaxForElementNotPresent(registerLink);
    }

}
