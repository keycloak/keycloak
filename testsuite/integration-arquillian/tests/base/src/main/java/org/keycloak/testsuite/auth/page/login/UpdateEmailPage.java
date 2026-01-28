/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.auth.page.login;

import org.keycloak.models.UserModel;
import org.keycloak.testsuite.pages.LogoutSessionsPage;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

public class UpdateEmailPage extends LogoutSessionsPage {

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "input-error-email")
    private WebElement inputErrorEmail;

    @FindBy(id = "kc-cancel")
    private WebElement cancelActionButton;

    @FindBy(id = "kc-submit")
    private WebElement submitButton;

    @FindBy(className = "kc-feedback-text")
    private WebElement feedbackMessage;

    @Override
    public boolean isCurrent() {
        return driver.getCurrentUrl().contains("login-actions/required-action")
                && driver.getCurrentUrl().contains("execution=" + UserModel.RequiredAction.UPDATE_EMAIL.name());
    }

    public void changeEmail(String email){
        emailInput.clear();
        emailInput.sendKeys(email);

        clickLink(submitButton);
    }

    public String getEmail() {
        return emailInput.getAttribute("value");
    }

    public String getEmailInputError() {
        try {
            return getTextFromElement(inputErrorEmail);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public boolean isCancelDisplayed() {
        try {
            return cancelActionButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public void clickCancelAIA() {
        clickLink(cancelActionButton);
    }

    public void clickSubmitAction() {
        clickLink(submitButton);
    }

    public String getInfo() {
        try {
            return getTextFromElement(feedbackMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

}
