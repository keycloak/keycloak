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

package org.keycloak.testsuite.auth.page.account;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElementIsNotPresent;

/**
 *
 * @author tkyjovsk
 */
public class AccountFields extends Form {

    @FindBy(id = "username")
    private WebElement usernameInput;
    @FindBy(id = "email")
    private WebElement emailInput;
    @FindBy(id = "firstName")
    private WebElement firstNameInput;
    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    public void setUsername(String username) {
        Form.setInputValue(usernameInput, username);
    }

    public AccountFields setEmail(String email) {
        Form.setInputValue(emailInput, email);
        return this;
    }

    public AccountFields setFirstName(String firstName) {
        Form.setInputValue(firstNameInput, firstName);
        return this;
    }

    public AccountFields setLastName(String lastName) {
        Form.setInputValue(lastNameInput, lastName);
        return this;
    }

    public String getEmail() {
        return Form.getInputValue(emailInput);
    }

    public String getFirstName() {
        return Form.getInputValue(firstNameInput);
    }

    public String getLastName() {
        return Form.getInputValue(lastNameInput);
    }

    public void setValues(UserRepresentation user) {
        setUsername(user.getUsername());
        setEmail(user.getEmail());
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
    }

    public void waitForUsernameInputPresent() {
        waitUntilElement(usernameInput).is().present();
    }

    public void waitForUsernameInputNotPresent() {
        waitUntilElementIsNotPresent(usernameInput);
    }

}
