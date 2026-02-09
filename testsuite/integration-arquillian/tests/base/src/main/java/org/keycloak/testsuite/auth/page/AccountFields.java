/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.auth.page;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.UIUtils;

import org.jboss.arquillian.graphene.page.Page;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * @author tkyjovsk
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class AccountFields extends FieldsBase {

    @Page
    private AccountErrors inputErrors;

    @FindBy(id = "username")
    private WebElement usernameInput;
    @FindBy(xpath = "//label[@for='username']")
    private WebElement usernameLabel;

    @FindBy(id = "email")
    private WebElement emailInput;
    @FindBy(xpath = "//label[@for='email']")
    private WebElement emailLabel;

    @FindBy(id = "firstName")
    private WebElement firstNameInput;
    @FindBy(xpath = "//label[@for='firstName']")
    private WebElement firstNameLabel;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;
    @FindBy(xpath = "//label[@for='lastName']")
    private WebElement lastNameLabel;

    public void setUsername(String username) {
        UIUtils.setTextInputValue(usernameInput, username);
    }

    public AccountFields setEmail(String email) {
        UIUtils.setTextInputValue(emailInput, email);
        return this;
    }

    public AccountFields setFirstName(String firstName) {
        UIUtils.setTextInputValue(firstNameInput, firstName);
        return this;
    }

    public AccountFields setLastName(String lastName) {
        UIUtils.setTextInputValue(lastNameInput, lastName);
        return this;
    }

    public String getUsername() {
        return UIUtils.getTextInputValue(usernameInput);
    }

    public String getEmail() {
        return UIUtils.getTextInputValue(emailInput);
    }

    public String getFirstName() {
        return UIUtils.getTextInputValue(firstNameInput);
    }

    public String getLastName() {
        return UIUtils.getTextInputValue(lastNameInput);
    }

    public void setValues(UserRepresentation user) {
        setUsername(user.getUsername());
        setEmail(user.getEmail());
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
    }

    public boolean isUsernamePresent() {
        try {
            return usernameInput.isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getUsernameLabel() {
        return getTextFromElement(usernameLabel);
    }

    public String getEmailLabel() {
        return getTextFromElement(emailLabel);
    }

    public String getFirstNameLabel() {
        return getTextFromElement(firstNameLabel);
    }

    public String getLastNameLabel() {
        return getTextFromElement(lastNameLabel);
    }

    public boolean hasUsernameError() {
        return inputErrors.getUsernameError() != null;
    }

    public boolean hasEmailError() {
        return inputErrors.getEmailError() != null;
    }

    public boolean hasFirstNameError() {
        return inputErrors.getFirstNameError() != null;
    }

    public boolean hasLastNameError() {
        return inputErrors.getLastNameError() != null;
    }

    public AccountErrors getInputErrors(){
        return inputErrors;
    }

    public static class AccountErrors{

        @FindBy(id = "input-error-firstname")
        private WebElement firstNameError;
        
        @FindBy(id = "input-error-firstName")
        private WebElement firstNameDynamicError;

        @FindBy(id = "input-error-lastname")
        private WebElement lastNameError;
        
        @FindBy(id = "input-error-lastName")
        private WebElement lastNameDynamicError;

        @FindBy(id = "input-error-email")
        private WebElement emailError;

        @FindBy(id = "input-error-username")
        private WebElement usernameError;

        @FindBy(id = "input-error-terms-accepted")
        private WebElement termsError;

        public String getFirstNameError() {
            try {
                return getTextFromElement(firstNameError);
            } catch (NoSuchElementException e) {
                try {
                    return getTextFromElement(firstNameDynamicError);
                } catch (NoSuchElementException ex) {
                    return null;
                }
            }
        }

        public String getLastNameError() {
            try {
                return getTextFromElement(lastNameError);
            } catch (NoSuchElementException e) {
                try {
                    return getTextFromElement(lastNameDynamicError);
                } catch (NoSuchElementException ex) {
                    return null;
                }
            }
        }

        public String getEmailError() {
            try {
                return getTextFromElement(emailError);
            } catch (NoSuchElementException e) {
                return null;
            }
        }

        public String getUsernameError() {
            try {
                return getTextFromElement(usernameError);
            } catch (NoSuchElementException e) {
                return null;
            }
        }

        public String getTermsError(){
            try {
                return getTextFromElement(termsError);
            } catch (NoSuchElementException e) {
                return null;
            }
        }
    }
}
