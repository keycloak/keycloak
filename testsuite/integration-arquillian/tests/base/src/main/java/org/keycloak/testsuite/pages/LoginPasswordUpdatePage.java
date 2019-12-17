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

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginPasswordUpdatePage extends LanguageComboboxAwarePage {

    @FindBy(id = "password-new")
    private WebElement newPasswordInput;

    @FindBy(id = "password-confirm")
    private WebElement passwordConfirmInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(className = "alert-error")
    private WebElement loginErrorMessage;

    @FindBy(xpath = "//span[@class='kc-feedback-text']")
    private WebElement feedbackMessage;
    
    @FindBy(name = "cancel-aia")
    private WebElement cancelAIAButton;

    public void changePassword(String newPassword, String passwordConfirm) {
        newPasswordInput.sendKeys(newPassword);
        passwordConfirmInput.sendKeys(passwordConfirm);

        submitButton.click();
    }
    
    public void cancel() {
        cancelAIAButton.click();
    }

    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).equals("Update password");
    }

    public void open() {
        throw new UnsupportedOperationException();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    public String getFeedbackMessage() {
        return feedbackMessage.getText();
    }

    public boolean isCancelDisplayed() {
        try {
            return cancelAIAButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

}
