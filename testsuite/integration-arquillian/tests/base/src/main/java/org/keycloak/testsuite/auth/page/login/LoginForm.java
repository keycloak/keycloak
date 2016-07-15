/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.auth.page.login;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.page.Form;
import static org.keycloak.testsuite.admin.Users.getPasswordOf;
import org.keycloak.testsuite.auth.page.account.AccountFields;
import org.keycloak.testsuite.auth.page.account.PasswordFields;
import static org.keycloak.testsuite.util.WaitUtils.*;

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
    @Page
    private TotpSetupForm totpForm;

    @FindBy(name = "login")
    private WebElement loginButton;
//    @FindBy(name = "cancel")
//    private WebElement cancelButton;

    @FindBy(xpath = "//div[@id='kc-registration']/span/a")
    private WebElement registerLink;
    @FindBy(linkText = "Forgot Password?")
    private WebElement forgottenPassword;
    @FindBy(id = "rememberMe")
    private WebElement rememberMe;

    @FindBy(xpath = ".//label[@for='password']")
    private WebElement labelPassword;

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
        registerLink.click();
        waitForPageToLoad(driver);
    }

    public void login() {
        labelPassword.click(); // This is a nasty trick for IE; As IE was "moving the cursor" towards the login button
                               // it opened the internationalization menu (when present) and then clicked
                               // one of the languages instead of the Login button
        loginButton.click();
        waitForPageToLoad(driver);
    }

    public void forgotPassword() {
        forgottenPassword.click();
        waitForPageToLoad(driver);
    }

    public void rememberMe(boolean value) {
        boolean selected = rememberMe.isSelected();
        if ((value && !selected) || !value && selected) {
            rememberMe.click();
            waitForPageToLoad(driver);
        }
    }

//    @Override
//    public void cancel() {
//        waitUntilElement(cancelButton).is().present();
//        cancelButton.click();
//    }
    public void waitForUsernameInputPresent() {
        accountFields.waitForUsernameInputPresent();
    }

    public void waitForRegisterLinkNotPresent() {
        waitUntilElementIsNotPresent(driver, registerLink);
    }

    public void waitForResetPasswordLinkNotPresent() {
        waitUntilElement(forgottenPassword).is().not().present();
    }

    public void waitForRememberMePresent() {
        waitUntilElement(rememberMe).is().present();
    }

    public void waitForRememberMeNotPresent() {
        waitUntilElementIsNotPresent(driver, rememberMe);
    }

    public void waitForLoginButtonPresent() {
        waitUntilElement(loginButton).is().present();
    }
    
    public TotpSetupForm totpForm() {
        return totpForm;
    }
    
    public class TotpSetupForm extends Form {
        @FindBy(id = "totp")
        private WebElement totpInputField;
        
        @FindBy(id = "totpSecret")
        private WebElement totpSecret;
        
        @FindBy(xpath = ".//input[@value='Submit']")
        private WebElement submit;
        
        public void waitForTotpInputFieldPresent() {
            waitUntilElement(totpInputField).is().present();
        }
        
        public void setTotp(String value) {
            setInputValue(totpInputField, value);
        }
        
        public String getTotpSecret() {
            return totpSecret.getAttribute(VALUE);
        }
        
        public void submit() {
            submit.click();
            waitForPageToLoad(driver);
        }
    }
}
