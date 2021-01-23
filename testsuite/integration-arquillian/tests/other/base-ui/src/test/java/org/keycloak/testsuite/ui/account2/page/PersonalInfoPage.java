/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui.account2.page;

import org.keycloak.representations.idm.UserRepresentation;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.UIAssert.assertElementDisabled;
import static org.keycloak.testsuite.util.UIAssert.assertInputElementValid;
import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextInputValue;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;
import static org.keycloak.testsuite.util.UIUtils.setTextInputValue;

import static org.junit.Assert.assertEquals;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class PersonalInfoPage extends AbstractLoggedInPage {
    @FindBy(id = "user-name")
    private WebElement username;
    @FindBy(id = "email-address")
    private WebElement email;
    @FindBy(id = "first-name")
    private WebElement firstName;
    @FindBy(id = "last-name")
    private WebElement lastName;
    @FindBy(id = "locale-select")
    private Select localeSelector;
    @FindBy(id = "save-btn")
    private WebElement saveBtn;
    @FindBy(id = "cancel-btn")
    private WebElement cancelBtn;
    @FindBy(id = "delete-account")
    private WebElement deleteAccountSection;

    @Override
    public String getPageId() {
        return "personal-info";
    }

    public void assertUsernameDisabled(boolean expected) {
        assertElementDisabled(expected, username);
    }

    public String getUsername() {
        return getTextInputValue(username);
    }

    public void setUsername(String value) {
        setTextInputValue(username, value);
    }

    public void assertUsernameValid(boolean expected) {
        assertInputElementValid(expected, username);
    }

    public String getEmail() {
        return getTextInputValue(email);
    }

    public void setEmail(String value) {
        setTextInputValue(email, value);
    }

    public void assertEmailValid(boolean expected) {
        assertInputElementValid(expected, email);
    }

    public String getFirstName() {
        return getTextInputValue(firstName);
    }

    public void setFirstName(String value) {
        setTextInputValue(firstName, value);
    }

    public void assertFirstNameValid(boolean expected) {
        assertInputElementValid(expected, firstName);
    }

    public String getLastName() {
        return getTextInputValue(lastName);
    }

    public void setLastName(String value) {
        setTextInputValue(lastName, value);
    }

    public void assertLastNameValid(boolean expected) {
        assertInputElementValid(expected, lastName);
    }

    public void assertSaveDisabled(boolean expected) {
        assertElementDisabled(expected, saveBtn);
    }

    public void assertDeleteAccountSectionVisible(boolean expected) {
      if (deleteAccountSection == null) {
        assertFalse(expected);
        return;
      }
        assertEquals(expected, isElementVisible(deleteAccountSection));
    }

    public void clickSave() {
        clickSave(true);
    }

    public void clickSave(boolean assertAlert) {
        saveBtn.click();
        if (assertAlert) {
            alert().assertIsDisplayed();
        }
    }

    public void clickCancel() {
        cancelBtn.click();
    }

    public void clickOpenDeleteExapandable() {
        clickLink(driver.findElement(By.cssSelector(".pf-c-expandable__toggle")));
    }

    public void clickDeleteAccountButton() {
      clickLink(driver.findElement(By.id("delete-account-btn")));
    }

    public void setValues(UserRepresentation user, boolean includeUsername) {
        if (includeUsername) {setUsername(user.getUsername());}
        setEmail(user.getEmail());
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
    }

    public boolean valuesEqual(UserRepresentation user) {
        return user.getUsername().equals(getUsername())
                && user.getEmail().equals(getEmail())
                && user.getFirstName().equals(getFirstName())
                && user.getLastName().equals(getLastName());
    }

    public void selectLocale(String customLocale) {
        localeSelector.selectByValue(customLocale);
    }
}
