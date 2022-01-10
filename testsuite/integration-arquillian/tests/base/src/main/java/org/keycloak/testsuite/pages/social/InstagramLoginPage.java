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

import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class InstagramLoginPage extends AbstractSocialLoginPage {
    @FindBy(name = "username")
    private WebElement usernameInput;

    @FindBy(name = "password")
    private WebElement passwordInput;

    @FindBy(xpath = "//button[text()='Save Info']")
    private WebElement saveInfoBtn;

    @FindBy(xpath = "//button[text()='Authorize']")
    private WebElement authorizeBtn;

    @FindBy(xpath = "//button[text()='Continue']")
    private WebElement continueBtn;

    @Override
    public void login(String user, String password) {
        try {
            usernameInput.clear();
            usernameInput.sendKeys(user);
            passwordInput.sendKeys(password);
            passwordInput.sendKeys(Keys.RETURN);
            pause(2000); // wait for the login screen a bit

            try {
                saveInfoBtn.click();
            }
            catch (NoSuchElementException e) {
                log.info("'Save Info' button not found, ignoring");
                pause(2000); // wait for the login screen a bit
            }
        }
        catch (NoSuchElementException e) {
            log.info("Instagram is already logged in, just confirmation is expected");
        }

        try {
            continueBtn.click();
        }
        catch (NoSuchElementException e) {
            log.info("'Continue' button not found, trying 'Authorize'...");
            authorizeBtn.click();
        }
    }
}
