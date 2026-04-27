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

    @FindBy(xpath = "//button[text()='Save info']")
    private WebElement saveInfoBtn;

    @FindBy(xpath = "//div[text()='Not now']")
    private WebElement notNowBtn;

    @FindBy(xpath = "//div[@aria-label='Allow']")
    private WebElement allowBtn;

    @Override
    public void login(String user, String password) {
        try {
            usernameInput.clear();
            usernameInput.sendKeys(user);
            passwordInput.sendKeys(password);
            passwordInput.sendKeys(Keys.RETURN);
            pause(5000);

            try {
                WaitUtils.waitUntilElement(saveInfoBtn).is().visible();
                saveInfoBtn.click();
                pause(3000);
            }
            catch (NoSuchElementException e) {
                log.info("'Save info' button not found, ignoring");
                pause(3000);
            }
        }
        catch (NoSuchElementException e) {
            log.info("Instagram is already logged in, just confirmation is expected");
        }

        // Approval dialog
        try {
            WaitUtils.waitUntilElement(allowBtn).is().visible();
            allowBtn.click();
        } catch (NoSuchElementException e) {
            log.info("'Allow' button not found, ignoring");
        }
    }

}
