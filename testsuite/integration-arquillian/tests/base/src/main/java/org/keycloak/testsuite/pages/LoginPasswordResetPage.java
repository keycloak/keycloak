/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.UIUtils;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginPasswordResetPage extends LanguageComboboxAwarePage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "input-error-username")
    private WebElement usernameError;

    @FindBy(css = "[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(css = "[class='pf-v5-c-success'], [class='alert-success']")
    private WebElement emailSuccessMessage;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement emailErrorMessage;

    @FindBy(partialLinkText = "Back to Login")
    private WebElement backToLogin;

    @FindBy(id = "kc-info-wrapper")
    private WebElement infoWrapper;

    @FindBy(id = "kc-reset-password-form")
    private WebElement formResetPassword;

    public void changePassword() {
        UIUtils.clickLink(submitButton);
    }

    public void changePassword(String username) {
        usernameInput.clear();
        usernameInput.sendKeys(username);

        UIUtils.clickLink(submitButton);
    }

    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).equals("Forgot Your Password?");
    }

    public String getSuccessMessage() {
        return emailSuccessMessage != null ? emailSuccessMessage.getText() : null;
    }

    public String getUsernameError() {
        try {
            return UIUtils.getTextFromElement(usernameError);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getErrorMessage() {
        try {
            return UIUtils.getTextFromElement(emailErrorMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public void backToLogin() {
        UIUtils.clickLink(backToLogin);
    }

    public String getInfoMessage() {
        try {
            return UIUtils.getTextFromElement(infoWrapper);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getFormUrl() {
            return formResetPassword.getAttribute("action");
    }
}
