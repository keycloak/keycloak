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
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class LinkedInLoginPage extends AbstractSocialLoginPage {
    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(xpath = "//button[(@type='submit') and (@aria-label='Sign in')]")
    private WebElement loginButton;

    @FindBy(name = "action")
    private WebElement authorizeButton;

    @Override
    public void login(String user, String password) {
        usernameInput.clear();
        usernameInput.sendKeys(user);
        passwordInput.sendKeys(password);
        loginButton.click();

        try {
            authorizeButton.click();
            log.info("LinkedIn test app authorized");
        }
        catch (NoSuchElementException e) {
            log.info("Skipping LinkedIn app authorization");
        }
    }
}
