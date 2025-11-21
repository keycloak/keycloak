/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.auth.page;

import org.keycloak.testsuite.util.UIUtils;

import org.jboss.arquillian.graphene.page.Page;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * @author tkyjovsk
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class PasswordFields extends FieldsBase {

    @Page
    private PasswordErrors inputErrors;

    @FindBy(id = "password")
    private WebElement passwordInput;
    @FindBy(xpath = "//label[@for='password']")
    private WebElement passwordLabel;

    @FindBy(id = "password-new")
    private WebElement newPasswordInput;
    @FindBy(xpath = "//label[@for='password-new']")
    private WebElement newPasswordLabel;

    @FindBy(id = "password-confirm")
    private WebElement confirmPasswordInput;
    @FindBy(xpath = "//label[@for='password-confirm']")
    private WebElement confirmPasswordLabel;

    public void setPassword(String password) {
        UIUtils.setTextInputValue(passwordInput, password);
    }

    public void setNewPassword(String newPassword) {
        UIUtils.setTextInputValue(newPasswordInput, newPassword);
    }

    public void setConfirmPassword(String confirmPassword) {
        UIUtils.setTextInputValue(confirmPasswordInput, confirmPassword);
    }

    public void setPasswords(String password, String newPassword, String confirmPassword) {
        setPassword(password);
        setNewPassword(newPassword);
        setConfirmPassword(confirmPassword);
    }

    public boolean isConfirmPasswordPresent() {
        try {
            return confirmPasswordInput.isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getPasswordLabel() {
        return getTextFromElement(passwordLabel);
    }

    public String getNewPasswordLabel() {
        return getTextFromElement(newPasswordLabel);
    }

    public String getConfirmPasswordLabel() {
        return getTextFromElement(confirmPasswordLabel);
    }

    public boolean hasPasswordError() {
        return inputErrors.getPasswordError() != null;
    }

    public boolean hasNewPasswordError() {
        return hasFieldError(newPasswordInput);
    }

    public boolean hasConfirmPasswordError() {
        return inputErrors.getPasswordConfirmError() != null;
    }

    public PasswordErrors getInputErrors() {
        return inputErrors;
    }

    public static class PasswordErrors {
        @FindBy(id = "input-error-password")
        private WebElement passwordError;

        @FindBy(id = "input-error-password-confirm")
        private WebElement passwordConfirmError;


        public String getPasswordError() {
            try {
                return getTextFromElement(passwordError);
            } catch (NoSuchElementException e) {
                return null;
            }
        }

        public String getPasswordConfirmError() {
            try {
                return getTextFromElement(passwordConfirmError);
            } catch (NoSuchElementException e) {
                return null;
            }
        }
    }
}
