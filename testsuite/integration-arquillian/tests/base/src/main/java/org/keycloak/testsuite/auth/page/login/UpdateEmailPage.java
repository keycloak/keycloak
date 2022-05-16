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

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

import org.keycloak.models.UserModel;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class UpdateEmailPage extends RequiredActions {

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "input-error-email")
    private WebElement inputErrorEmail;

    @FindBy(css = "button[name='cancel-aia']")
    private WebElement cancelActionButton;

    @FindBy(css = "input[type='submit']")
    private WebElement submitActionButton;

    @Override
    public String getActionId() {
        return UserModel.RequiredAction.UPDATE_EMAIL.name();
    }

    @Override
    public boolean isCurrent() {
        return driver.getCurrentUrl().contains("login-actions/required-action")
                && driver.getCurrentUrl().contains("execution=" + getActionId());
    }

    public void changeEmail(String email){
        emailInput.clear();
        emailInput.sendKeys(email);

        submit();
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
        clickLink(submitActionButton);
    }

}
