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

package org.keycloak.testsuite.admin.page.account;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author Petr Mensik
 */
public class PasswordPage {

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-new")
    private WebElement newPasswordInput;

    @FindBy(id = "password-confirm")
    private WebElement confirmInput;

    @FindByJQuery("button[value='Save']")
    private WebElement save;

    @FindBy(xpath = "//input[@value='Submit']")
    private WebElement submit; // on "update password" page, after first login

    public void setPassword(String oldPassword, String newPassword) {
        passwordInput.clear();
        passwordInput.sendKeys(oldPassword);
        confirmNewPassword(newPassword);
    }
    
    public void confirmNewPassword(String newPassword) {
        newPasswordInput.clear();
        newPasswordInput.sendKeys(newPassword);
        confirmInput.clear();
        confirmInput.sendKeys(newPassword);
    }
    
    public void setOldPasswordField(String oldPassword) {
        passwordInput.clear();
        passwordInput.sendKeys(oldPassword);
    }

    public void setNewPasswordField(String newPassword) {
        newPasswordInput.clear();
        newPasswordInput.sendKeys(newPassword);
    }

    public void setConfirmField(String confirmPassword) {
        confirmInput.clear();
        confirmInput.sendKeys(confirmPassword);
    }

    public void save() {
        save.click();
    }

    public void submit() {
        submit.click();
    }
    
}
