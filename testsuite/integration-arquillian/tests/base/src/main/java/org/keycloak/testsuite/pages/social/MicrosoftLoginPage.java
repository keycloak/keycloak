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
public class MicrosoftLoginPage extends AbstractSocialLoginPage {
    @FindBy(name = "loginfmt")
    private WebElement usernameInput;

    @FindBy(name = "passwd")
    private WebElement passwordInput;

    @FindBy(id = "idSIButton9")
    private WebElement submitButton;

    @FindBy(id = "acceptButton")
    private WebElement acceptButton;

    @FindBy(xpath = "//input[contains(@class,'btn-primary')]")
    private WebElement appAccessButton;

    @Override
    public void login(String user, String password) {
        WaitUtils.pause(5000); // we need to take it a bit slower
        usernameInput.clear();
        usernameInput.sendKeys(user);
        submitButton.click();

        WaitUtils.pause(5000);
        try {
            passwordInput.sendKeys(password);
            submitButton.click();
        }
        catch (NoSuchElementException e) {
            log.info("Already logged in to Microsoft IdP, no need to enter password");
        }

        // While logging into the app for the first time user is asked if he wants to stay signed in
        try {
            WaitUtils.pause(3000);
            acceptButton.click();
        }
        catch (NoSuchElementException e) {
            log.info("User already allowed in the app");
        }

        // The app requires user consent for access to their information
        try {
            WaitUtils.pause(3000);
            appAccessButton.click();
        }
        catch (NoSuchElementException e) {
            log.info("App already has access to user information");
        }
    }
}
