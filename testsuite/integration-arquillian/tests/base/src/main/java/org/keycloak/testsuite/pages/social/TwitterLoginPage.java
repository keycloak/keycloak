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

import java.util.List;

import org.keycloak.testsuite.util.WaitUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class TwitterLoginPage extends AbstractSocialLoginPage {
    private static final String signInBtnLocator = "allow";

    @FindBy(xpath = "//input[@type='text']")
    private WebElement usernameInput;

    @FindBy(xpath = "//input[@type='password']")
    private WebElement passwordInput;

    @FindBy(id = signInBtnLocator)
    private WebElement signInBtn;

    @Override
    public void login(String user, String password) {
        // new login page is two phase login (username and then password) and it
        // needs lots of JS, twitter does not work with default HtmlUnit driver

       List<WebElement> signInBtn = driver.findElements(By.id(signInBtnLocator));
        if (signInBtn.size() > 0) {
            signInBtn.get(0).click();
        }

        pause(3000);
        usernameInput.clear();
        usernameInput.sendKeys(user);
        usernameInput.sendKeys(Keys.RETURN);

        // wait for the password input to appear
        WaitUtils.waitUntilElement(passwordInput).is().visible();
        passwordInput.clear();
        passwordInput.sendKeys(password);
        passwordInput.sendKeys(Keys.RETURN);

    }
}
