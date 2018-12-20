/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * @author Wladislaw Mitzel <wmitzel@tawadi.de>
 */
public class VKontakteLoginPage extends AbstractSocialLoginPage {

    @FindBy(name = "email")
    private WebElement usernameInput;

    @FindBy(name = "pass")
    private WebElement passwordInput;

    @FindBy(id = "install_allow")
    private WebElement loginButton;

    @FindBy(xpath = "//button[text()='Allow']")
    private WebElement authorizeButton;

    @Override
    public void login(String user, String password) {
        //normal login
        try {
            usernameInput.clear();
            usernameInput.sendKeys(user);
            passwordInput.sendKeys(password);
            clickLink(loginButton);
        } catch (NoSuchElementException e) {
            // already logged in
        }
        //scope confirmation
        try {
            authorizeButton.click();
        } catch (NoSuchElementException e) {
            // might not be necessary
        }
    }
}
