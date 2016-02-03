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
package org.keycloak.testsuite.auth.page.account;

import org.keycloak.testsuite.page.Form;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class PasswordFields extends Form {

    @FindBy(id = "password")
    private WebElement passwordInput;
    @FindBy(id = "password-new")
    private WebElement newPasswordInput;
    @FindBy(id = "password-confirm")
    private WebElement confirmPasswordInput;

    public void setPassword(String password) {
        setInputValue(passwordInput, password);
    }

    public void setNewPassword(String newPassword) {
        setInputValue(newPasswordInput, newPassword);
    }

    public void setConfirmPassword(String confirmPassword) {
        setInputValue(confirmPasswordInput, confirmPassword);
    }

    public void setPasswords(String password, String newPassword, String confirmPassword) {
        setPassword(password);
        setNewPassword(newPassword);
        setConfirmPassword(confirmPassword);
    }

    public void waitForConfirmPasswordInputPresent() {
        waitUntilElement(confirmPasswordInput).is().present();
    }
}
