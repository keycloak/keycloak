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

import java.util.Map;
import java.util.Map.Entry;

import org.keycloak.models.Constants;
import org.keycloak.testsuite.auth.page.AccountFields;
import org.keycloak.testsuite.auth.page.PasswordFields;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.UIUtils;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RegisterPage extends LanguageComboboxAwarePage
{

    @Page
    private AccountFields.AccountErrors accountErrors;

    @Page
    private PasswordFields.PasswordErrors passwordErrors;

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

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement loginAlertErrorMessage;

    @FindBy(className = "instruction")
    private WebElement loginInstructionMessage;

    @FindBy(linkText = "« Back to Login")
    private WebElement backToLoginLink;

    public void register(String firstName, String lastName, String email, String username, String password) {
        register(firstName, lastName, email, username, password, password, null, null, null);
    }

    public void register(String firstName, String lastName, String email, String username, String password, String passwordConfirm) {
        register(firstName, lastName, email, username, password, passwordConfirm, null, null, null);
    }

    public void register(String firstName, String lastName, String email, String username, String password, String passwordConfirm, String department) {
        register(firstName, lastName, email, username, password, passwordConfirm, department, null, null);
    }

    public void register(String firstName, String lastName, String email, String username, String password, String passwordConfirm, Map<String, String> attributes) {
        register(firstName, lastName, email, username, password, passwordConfirm, null, null, attributes);
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

        if (isEmailPresent()) {
            emailInput.clear();
            if (email != null) {
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

        if(isDepartmentPresent()) {
            departmentInput.clear();
            if (department != null) {
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

        UIUtils.clickLink(submitButton);
    }

    public void registerWithEmailAsUsername(String firstName, String lastName, String email, String password) {
        registerWithEmailAsUsername(firstName, lastName, email, password, password);
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

        UIUtils.clickLink(submitButton);
    }

    public void clickBackToLogin() {
        UIUtils.clickLink(backToLoginLink);
    }

    public String getAlertError() {
        try {
            return UIUtils.getTextFromElement(loginAlertErrorMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getInstruction() {
        try {
            return UIUtils.getTextFromElement(loginInstructionMessage);
        } catch (NoSuchElementException e){
            // OK
        }
        return null;
    }

    public String getLabelForField(String fieldId) {
        return driver.findElement(By.cssSelector("label[for="+fieldId+"]")).getText().replaceAll("\\s\\*$", "");
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

    public String getDepartment() {
        return departmentInput.getAttribute("value");
    }

    public boolean isDepartmentEnabled() {
        return departmentInput.isEnabled();
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

    public boolean isUsernamePresent() {
        try {
            return driver.findElement(By.name("username")).isDisplayed();
        } catch (NoSuchElementException nse) {
            return false;
        }
    }


    public boolean isCurrent() {
        return isCurrent("Register");
    }

    public AccountFields.AccountErrors getInputAccountErrors(){
        return accountErrors;
    }

    public PasswordFields.PasswordErrors getInputPasswordErrors(){
        return passwordErrors;
    }

    public void openWithLoginHint(String loginHint) {
        oauth.registrationForm().loginHint(loginHint).open();
        assertCurrent();
    }

    public void assertCurrent(String orgName) {
        String name = getClass().getSimpleName();
        Assert.assertTrue("Expected " + name + " but was " + DroneUtils.getCurrentDriver().getTitle() + " (" + DroneUtils.getCurrentDriver().getCurrentUrl() + ")",
                isCurrent("Create an account to join the " + orgName + " organization"));
    }
}
