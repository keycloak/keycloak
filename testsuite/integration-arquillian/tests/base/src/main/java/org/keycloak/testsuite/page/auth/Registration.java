/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.page.auth;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.UriBuilder;
import org.keycloak.representations.idm.UserRepresentation;

import static org.keycloak.testsuite.util.SeleniumUtils.waitGuiForElement;
import static org.keycloak.testsuite.util.Users.getPasswordCredentialOf;

/**
 *
 * @author Filip Kiss
 */
public class Registration extends LoginActions {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("registration");
    }

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-confirm")
    private WebElement passwordConfirmInput;

    @FindBy(css = "span.kc-feedback-text")
    private WebElement feedbackError;

    @FindBy(css = "div[id='kc-form-options'] span a")
    private WebElement backToLoginForm;

    @FindBy(xpath = "//input[@type='submit']")
    private WebElement registerButton;

    public void registerNewUser(UserRepresentation user) {
        registerNewUser(user, getPasswordCredentialOf(user).getValue());
    }

    public void registerNewUser(UserRepresentation user, String confirmPassword) {
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
        waitGuiForElement(passwordConfirmInput, "Register form should be visible");
        clearAndType(usernameInput, user.getUsername());
        clearAndType(firstNameInput, user.getFirstName());
        clearAndType(lastNameInput, user.getLastName());
        clearAndType(emailInput, user.getEmail());
        clearAndType(passwordInput, getPasswordCredentialOf(user).getValue());
        clearAndType(passwordConfirmInput, confirmPassword);
        registerButton.submit();
    }

    public void clearAndType(WebElement webElement, String text) {
        webElement.clear();
        webElement.sendKeys(text);
    }

    public boolean isInvalidEmail() {
        waitGuiForElement(feedbackError, "Feedback message should be visible");
        return feedbackError.getText().equals("Invalid email address.");
    }

    public boolean isAttributeSpecified(String attribute) {
        waitGuiForElement(feedbackError, "Feedback message should be visible");
        return !feedbackError.getText().contains("Please specify " + attribute + ".");
    }

    public boolean isPasswordSame() {
        waitGuiForElement(feedbackError, "Feedback message should be visible");
        return !feedbackError.getText().equals("Password confirmation doesn't match.");
    }

    public void backToLoginPage() {
        backToLoginForm.click();
    }

}
