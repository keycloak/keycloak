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

package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.auth.page.AccountFields;
import org.keycloak.testsuite.util.UIUtils;

import org.jboss.arquillian.graphene.page.Page;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
public class VerifyProfilePage extends AbstractPage {

    @Page
    private AccountFields.AccountErrors accountErrors;

    @FindBy(name = "firstName")
    private WebElement firstNameInput;

    @FindBy(name = "lastName")
    private WebElement lastNameInput;

    @FindBy(name = "email")
    private WebElement emailInput;

    @FindBy(name = "department")
    private WebElement departmentInput;


    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement loginAlertErrorMessage;


    public void update(String firstName, String lastName) {
        firstNameInput.clear();
        if (firstName != null) {
            firstNameInput.sendKeys(firstName);
        }

        lastNameInput.clear();
        if (lastName != null) {
            lastNameInput.sendKeys(lastName);
        }

        UIUtils.clickLink(submitButton);
    }

    public void update(String firstName, String lastName, String department) {
        departmentInput.clear();
        if (department != null) {
            departmentInput.sendKeys(department);
        }

        update(firstName, lastName);
    }

    public void updateEmail(String email, String firstName, String lastName) {

        emailInput.clear();
        if (emailInput != null) {
            emailInput.sendKeys(email);
        }

        firstNameInput.clear();
        if (firstName != null) {
            firstNameInput.sendKeys(firstName);
        }

        lastNameInput.clear();
        if (lastName != null) {
            lastNameInput.sendKeys(lastName);
        }

        UIUtils.clickLink(submitButton);
    }

    public String getAlertError() {
        try {
            return UIUtils.getTextFromElement(loginAlertErrorMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
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

    public String getDepartment() {
        return departmentInput.getAttribute("value");
    }

    public boolean isDepartmentEnabled() {
        return departmentInput.isEnabled();
    }

    public boolean isUsernamePresent() {
        try {
            return driver.findElement(By.id("username")).isDisplayed();
        } catch (NoSuchElementException nse) {
            return false;
        }
    }

    public boolean isEmailPresent() {
        try {
            return driver.findElement(By.id("email")).isDisplayed();
        } catch (NoSuchElementException nse) {
            return false;
        }
    }

    public boolean isUsernameEnabled() {
        try {
            return driver.findElement(By.id("username")).isEnabled();
        } catch (NoSuchElementException nse) {
            return false;
        }
    }

    public boolean isDepartmentPresent() {
        try {
          isDepartmentEnabled();
          return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getEmail() {
        return emailInput.getAttribute("value");
    }

    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).equals("Update Account Information");
    }

    public AccountFields.AccountErrors getInputAccountErrors(){
        return accountErrors;
    }

}
