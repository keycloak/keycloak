/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

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

    public UpdateEmailPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-update-email";
    }

    public void changeEmail(String email) {
        emailInput.clear();
        emailInput.sendKeys(email);
        submitButton.click();
    }

    public String getEmail() {
        return emailInput.getAttribute("value");
    }

    public String getEmailInputError() {
        try {
            return inputErrorEmail.getText();
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
        cancelActionButton.click();
    }

    public void clickSubmitAction() {
        submitButton.click();
    }

    public String getInfo() {
        try {
            return feedbackMessage.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

}
