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

import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class GitLabLoginPage extends AbstractSocialLoginPage {
    @FindBy(id = "user_login")
    private WebElement usernameInput;

    @FindBy(id = "user_password")
    private WebElement passwordInput;

    @FindBy(xpath = "//input[@name='commit' and @value='Authorize']")
    private WebElement authorizeButton;

    @Override
    public void login(String user, String password) {
        try {
            usernameInput.sendKeys(user);
            passwordInput.sendKeys(password);
            passwordInput.sendKeys(Keys.RETURN);
        }
        catch (NoSuchElementException e) {
            // already logged in
        }

        try {
            authorizeButton.click();
        }
        catch (NoSuchElementException e) {
            // might not be necessary
        }
    }
}
