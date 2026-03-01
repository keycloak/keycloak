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

package org.keycloak.testframework.ui.page;

import java.util.Map;
import java.util.Map.Entry;

import org.keycloak.models.Constants;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RegisterPage extends AbstractLoginPage {

    @FindBy(name = "firstName")
    private WebElement firstNameInput;

    @FindBy(name = "lastName")
    private WebElement lastNameInput;

    @FindBy(name = "email")
    private WebElement emailInput;

    @FindBy(name = "username")
    private WebElement usernameInput;

    @FindBy(name = "password")
    private WebElement passwordInput;

    @FindBy(name = "password-confirm")
    private WebElement passwordConfirmInput;

    @FindBy(name = "department")
    private WebElement departmentInput;

    @FindBy(name = "termsAccepted")
    private WebElement termsAcceptedInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    public RegisterPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void register(String firstName, String lastName, String email, String username, String password) {
        register(firstName, lastName, email, username, password, password, null, null, null);
    }

    public void register(String firstName, String lastName, String email, String username, String password, String passwordConfirm) {
        register(firstName, lastName, email, username, password, passwordConfirm, null, null, null);
    }

    public void register(String firstName, String lastName, String email, String username, String password, String passwordConfirm, String department, Boolean termsAccepted, Map<String, String> attributes) {
        firstNameInput.clear();
        if (firstName != null) {
            firstNameInput.sendKeys(firstName);
        }

        lastNameInput.clear();
        if (lastName != null) {
            lastNameInput.sendKeys(lastName);
        }

        if (email != null) {
            if (isEmailPresent()) {
                emailInput.clear();
                emailInput.sendKeys(email);
            }
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


        if (department != null) {
            if(isDepartmentPresent()) {
                departmentInput.clear();
                departmentInput.sendKeys(department);
            }
        }

        if (termsAccepted != null && termsAccepted) {
            termsAcceptedInput.click();
        }

        if (attributes != null) {
            for (Entry<String, String> attribute : attributes.entrySet()) {
                driver.findElement(By.name(Constants.USER_ATTRIBUTES_PREFIX + attribute.getKey())).sendKeys(attribute.getValue());
            }
        }

        submitButton.sendKeys(Keys.ENTER);
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

    public boolean isDepartmentPresent() {
        try {
            return driver.findElement(By.name("department")).isDisplayed();
        } catch (NoSuchElementException nse) {
            return false;
        }
    }

    public boolean isEmailPresent() {
        try {
            return driver.findElement(By.name("email")).isDisplayed();
        } catch (NoSuchElementException nse) {
            return false;
        }
    }

    @Override
    public String getExpectedPageId() {
        return "login-register";
    }
}
