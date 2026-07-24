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

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.URLUtils.navigateToUri;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class GoogleLoginPage extends AbstractSocialLoginPage {
    @FindBy(id = "identifierId")
    private WebElement emailInput;

    @FindBy(xpath = ".//input[@type='password']")
    private WebElement passwordInput;

    @FindBy(xpath = "//form//ul/li/div[@role='link']")
    private List<WebElement> selectAccountLinks;

    @Override
    public void login(String user, String password) {
        if (selectAccountLinks.size() > 1) {
            clickLink(selectAccountLinks.get(selectAccountLinks.size() - 1));
        }

        emailInput.clear();
        emailInput.sendKeys(user);
        emailInput.sendKeys(Keys.RETURN);
        pause(3000); // wait for some animation or whatever
        passwordInput.sendKeys(password);
        passwordInput.sendKeys(Keys.RETURN);
    }

    @Override
    public void logout() {
        log.info("performing logout from Google");
        navigateToUri("https://www.google.com/accounts/Logout");
    }
}
