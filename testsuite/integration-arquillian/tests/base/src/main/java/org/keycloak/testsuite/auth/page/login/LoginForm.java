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
import org.keycloak.testsuite.auth.page.AccountFields;
import org.keycloak.testsuite.auth.page.PasswordFields;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.admin.Users.getPasswordOf;
import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

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

    @FindBy(xpath = "//div[@id='kc-registration']/span/a")
    private WebElement registerLink;
    @FindBy(linkText = "Forgot Password?")
    private WebElement forgottenPassword;

    @FindBy(id = "rememberMe")
    private WebElement rememberMe;
    @FindBy(xpath = "//input[@id='rememberMe']/parent::label")
    private WebElement rememberMeLabel;

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
        clickLink(registerLink);
    }

    public void login() {
        clickLink(loginButton);
    }

    public void forgotPassword() {
        clickLink(forgottenPassword);
    }

    public void rememberMe(boolean value) {
        boolean selected = rememberMe.isSelected();
        if ((value && !selected) || !value && selected) {
            rememberMe.click();
        }
    }

    public boolean isRememberMe() {
        return rememberMe.isSelected();
    }

    public boolean isUsernamePresent() {
        return accountFields.isUsernamePresent();
    }

    public boolean isRegisterLinkPresent() {
        try {
            return registerLink.isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isForgotPasswordLinkPresent() {
        try {
            return forgottenPassword.isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isRememberMePresent() {
        try {
            return rememberMe.isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isLoginButtonPresent() {
        try {
            return loginButton.isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }
    
    public TotpSetupForm totpForm() {
        return totpForm;
    }

    public String getUsernameLabel() {
        return accountFields.getUsernameLabel();
    }

    public String getUsername() {
        return accountFields.getUsername();
    }

    public String getPasswordLabel() {
        return passwordFields.getPasswordLabel();
    }

    public String getRememberMeLabel() {
        return getTextFromElement(rememberMeLabel);
    }

    public static class TotpSetupForm extends Form {
        @FindBy(id = "totp")
        private WebElement totpInputField;

        @FindBy(id = "userLabel")
        private WebElement userLabelInputField;

        @FindBy(id = "totpSecret")
        private WebElement totpSecret;

        @FindBy(xpath = ".//input[@value='Submit']")
        private WebElement submit;

        public void setTotp(String value) {
            UIUtils.setTextInputValue(totpInputField, value);
        }

        public void setUserLabel(String value) {
            UIUtils.setTextInputValue(userLabelInputField, value);
        }
        
        public String getTotpSecret() {
            return totpSecret.getAttribute(UIUtils.VALUE_ATTR_NAME);
        }

        public void submit() {
            clickLink(submit);
        }
    }
}
