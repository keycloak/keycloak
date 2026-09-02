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

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class OpenShiftLoginPage extends AbstractSocialLoginPage {
    @FindBy(name = "username")
    private WebElement usernameInput;

    @FindBy(name = "password")
    private WebElement passwordInput;

    @FindBy(name = "approve")
    private WebElement authorizeButton;

    private String userLoginLinkTitle;

    private WebElement userLoginLink;

    @Override
    public void login(String user, String password) {
        if(userLoginLinkTitle != null) {
            setUserLoginLink(this.userLoginLinkTitle);
            if(this.userLoginLink != null) {
                clickLink(this.userLoginLink);
            }
        }

        WaitUtils.pause(3000);
        usernameInput.sendKeys(user);
        passwordInput.sendKeys(password);
        passwordInput.sendKeys(Keys.RETURN);

        try {
            WaitUtils.pause(3000);
            authorizeButton.click();
        }
        catch (NoSuchElementException e) {
            log.info("User already allowed in the app");
        }
    }

    public void setUserLoginLinkTitle(String title) {
        this.userLoginLinkTitle = title;
    }

    private void setUserLoginLink(String linkAttrTitle) {
        try {
            this.userLoginLink = driver.findElement(By.xpath("//a[contains(@title,'"+linkAttrTitle+"')]"));
        } catch (NoSuchElementException ex) {
            log.error("No link with title: '" + linkAttrTitle + "' found on page. If you use the OPENSHIFT4_KUBE_ADMIN provider, set property loginBtnTitle in properties file to an existing title on the page to fix this error.");
        }
    }
}
