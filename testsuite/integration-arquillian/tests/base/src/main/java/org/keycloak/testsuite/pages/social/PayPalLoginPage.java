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

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Petter Lysne (petterlysne at hotmail dot com)
 */
public class PayPalLoginPage extends AbstractSocialLoginPage {
    @FindBy(id = "email")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(name = "btnLogin")
    private WebElement loginButton;

    @FindBy(name = "continueLogin")
    private WebElement continueLoginButton;

    @Override
    public void login(String user, String password) {
        try {
            usernameInput.clear(); // to remove pre-filled email
            usernameInput.sendKeys(user);
            passwordInput.sendKeys(password);
            loginButton.click();
        }
        catch (NoSuchElementException e) {
            continueLoginButton.click(); // already logged in, just need to confirm it
        }
    }
}
