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

import org.keycloak.testsuite.util.URLUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author Petr Mensik
 */
public class Account extends AccountManagement {
    
    @FindBy(id = "username")
    private WebElement username;

    @FindBy(id = "email")
    private WebElement email;

    @FindBy(id = "lastName")
    private WebElement lastName;

    @FindBy(id = "firstName")
    private WebElement firstName;

    public String getUsername() {
        return username.getAttribute("value");
    }

    public String getEmail() {
        return email.getAttribute("value");
    }

    public String getFirstName() {
        return firstName.getAttribute("value");
    }

    public String getLastName() {
        return lastName.getAttribute("value");
    }

    public Account setUsername(String value) {
        username.clear();
        username.sendKeys(value);
        return this;
    }
    
    public Account setEmail(String value) {
        email.clear();
        email.sendKeys(value);
        return this;
    }

    public Account setFirstName(String value) {
        firstName.clear();
        firstName.sendKeys(value);
        return this;
    }

    public Account setLastName(String value) {
        lastName.clear();
        lastName.sendKeys(value);
        return this;
    }
    
    public boolean isCurrent() {
        return URLUtils.currentUrlStartsWith(toString());     // Sometimes after login the URL ends with /# or similar
    }

}
