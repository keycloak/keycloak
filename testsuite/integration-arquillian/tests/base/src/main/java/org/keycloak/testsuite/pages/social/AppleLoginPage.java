/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.pages.social;

import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class AppleLoginPage extends AbstractSocialLoginPage {
    @FindBy(id = "account_name_text_field")
    private WebElement usernameInput;

    @FindBy(id = "password_text_field")
    private WebElement passwordInput;

    @FindBy(id = "sign-in")
    private WebElement loginButton;

    @Override
    public void login(String user, String password) {
        try {
            usernameInput.clear();
            usernameInput.sendKeys(user);
            loginButton.click();
            passwordInput.sendKeys(password);
        }
        catch (NoSuchElementException e) { // at some conditions we are already logged in and just need to confirm it
        }
        finally {
            loginButton.click();

            // to allow 2FA code typing and agree Apple sign in questions
            WaitUtils.pause(30000);
        }
    }
}
