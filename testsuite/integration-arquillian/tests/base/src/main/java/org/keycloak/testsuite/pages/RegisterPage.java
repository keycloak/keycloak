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

import org.junit.Assert;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RegisterPage extends AbstractPage {

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-confirm")
    private WebElement passwordConfirmInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(className = "alert-error")
    private WebElement loginErrorMessage;

    @FindBy(className = "instruction")
    private WebElement loginInstructionMessage;

    @FindBy(linkText = "« Back to Login")
    private WebElement backToLoginLink;


    public void register(String firstName, String lastName, String email, String username, String password, String passwordConfirm) {
        firstNameInput.clear();
        if (firstName != null) {
            firstNameInput.sendKeys(firstName);
        }

        lastNameInput.clear();
        if (lastName != null) {
            lastNameInput.sendKeys(lastName);
        }

        emailInput.clear();
        if (email != null) {
            emailInput.sendKeys(email);
        }

        usernameInput.clear();
        if (username != null) {
            usernameInput.sendKeys(username);
        }

        passwordInput.clear();
        if (password != null) {
            passwordInput.sendKeys(password);
        }

        passwordConfirmInput.clear();
        if (passwordConfirm != null) {
            passwordConfirmInput.sendKeys(passwordConfirm);
        }

        submitButton.click();
    }

    public void registerWithEmailAsUsername(String firstName, String lastName, String email, String password, String passwordConfirm) {
        firstNameInput.clear();
        if (firstName != null) {
            firstNameInput.sendKeys(firstName);
        }

        lastNameInput.clear();
        if (lastName != null) {
            lastNameInput.sendKeys(lastName);
        }

        emailInput.clear();
        if (email != null) {
            emailInput.sendKeys(email);
        }

        try {
            usernameInput.clear();
            Assert.fail("Form must be without username field");
        } catch (NoSuchElementException e) {
            // OK
        }

        passwordInput.clear();
        if (password != null) {
            passwordInput.sendKeys(password);
        }

        passwordConfirmInput.clear();
        if (passwordConfirm != null) {
            passwordConfirmInput.sendKeys(passwordConfirm);
        }

        submitButton.click();
    }

    public void clickBackToLogin() {
        backToLoginLink.click();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    public String getInstruction() {
        try {
            return loginInstructionMessage != null ? loginInstructionMessage.getText() : null;
        } catch (NoSuchElementException e){
            // OK
        }
        return null;
    }

    public String getFirstName() {
        return firstNameInput.getAttribute("value");
    }

    public String getLastName() {
        return lastNameInput.getAttribute("value");
    }

    public String getEmail() {
        return emailInput.getAttribute("value");
    }

    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public String getPassword() {
        return passwordInput.getAttribute("value");
    }

    public String getPasswordConfirm() {
        return passwordConfirmInput.getAttribute("value");
    }

    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).equals("Register");
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }

}