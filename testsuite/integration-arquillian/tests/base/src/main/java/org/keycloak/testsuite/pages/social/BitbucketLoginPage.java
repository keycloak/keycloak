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

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class BitbucketLoginPage extends AbstractSocialLoginPage {
    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(xpath = "//div[contains(@class,'additional-auths')]/p/a")
    private WebElement loginWithAtlassianButton;

    @Override
    public void login(String user, String password) {
        try {
            clickLink(loginWithAtlassianButton);    // BitBucket no longer has it's own login page yet sometimes it's
                                                    // displayed even though we need to use the Atlassian login page
        }
        catch (NoSuchElementException e) {
            log.info("Already on Atlassian login page");
        }

        usernameInput.sendKeys(user);
        usernameInput.sendKeys(Keys.RETURN);
        pause(1000);

        passwordInput.sendKeys(password);
        passwordInput.sendKeys(Keys.RETURN);
    }
}
