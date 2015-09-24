/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

    @FindBy(name = "login")
    private WebElement loginButton;
//    @FindBy(name = "cancel")
//    private WebElement cancelButton;

    @FindBy(linkText = "Register")
    private WebElement registerLink;
    @FindBy(linkText = "Forgot Password?")
    private WebElement forgottenPassword;
    @FindBy(id = "rememberMe")
    private WebElement rememberMe;

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
    
    public void rememberMe(boolean value) {
        waitForRememberMePresent();
        boolean selected = rememberMe.isSelected();
        if ((value && !selected) || !value && selected) {
            rememberMe.click();
        }
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

    public void waitForResetPasswordLinkNotPresent() {
        waitAjaxForElementNotPresent(forgottenPassword);
    }

    public void waitForRememberMePresent() {
        waitAjaxForElement(rememberMe);
    }

    public void waitForRememberMeNotPresent() {
        waitAjaxForElementNotPresent(rememberMe);
    }
    
    public void waitForLoginButtonPresent() {
        waitGuiForElement(loginButton);
    }

}
