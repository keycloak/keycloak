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

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginUpdateProfilePage extends AbstractPage {

    @Page
    private UpdateProfileErrors errorsPage;

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "email")
    private WebElement emailInput;
    
    @FindBy(id = "department")
    private WebElement departmentInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;
    
    @FindBy(name = "cancel-aia")
    private WebElement cancelAIAButton;

    @FindBy(className = "alert-error")
    private WebElement loginAlertErrorMessage;

    public void update(String firstName, String lastName, String email) {
        updateWithDepartment(firstName, lastName, null, email);
    }
    
    public void updateWithDepartment(String firstName, String lastName, String department, String email) {
        if (firstName != null) {
            firstNameInput.clear();
            firstNameInput.sendKeys(firstName);
        }
        if (lastName != null) {
            lastNameInput.clear();
            lastNameInput.sendKeys(lastName);
        }
        if (email != null) {
            emailInput.clear();
            emailInput.sendKeys(email);
        }

        if(department != null) {
            departmentInput.clear();
            departmentInput.sendKeys(department);
        }
        
        clickLink(submitButton);
    }

    public void cancel() {
        cancelAIAButton.click();
    }

    public String getAlertError() {
        try {
            return UIUtils.getTextFromElement(loginAlertErrorMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
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
    
    public String getDepartment() {
        return departmentInput.getAttribute("value");
    }

    public boolean isDepartmentEnabled() {
        return departmentInput.isEnabled();
    }

    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).equals("Update Account Information");
    }

    public UpdateProfileErrors getInputErrors() {
        return errorsPage;
    }
    
    public String getLabelForField(String fieldId) {
        return driver.findElement(By.cssSelector("label[for="+fieldId+"]")).getText();
    }
    
    public boolean isDepartmentPresent() {
        try {
          isDepartmentEnabled();
          return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }

    public boolean isCancelDisplayed() {
        try {
            return cancelAIAButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    // For managing input errors
    public static class UpdateProfileErrors {

        @FindBy(id = "input-error-firstname")
        private WebElement inputErrorFirstName;

        @FindBy(id = "input-error-firstName")
        private WebElement inputErrorFirstNameDynamic;

        @FindBy(id = "input-error-lastname")
        private WebElement inputErrorLastName;
        
        @FindBy(id = "input-error-lastName")
        private WebElement inputErrorLastNameDynamic;

        @FindBy(id = "input-error-email")
        private WebElement inputErrorEmail;

        @FindBy(id = "input-error-username")
        private WebElement inputErrorUsername;

        public String getFirstNameError() {
            try {
                return getTextFromElement(inputErrorFirstName);
            } catch (NoSuchElementException e) {
                try {
                    return getTextFromElement(inputErrorFirstNameDynamic);
                } catch (NoSuchElementException ex) {
                    return null;
                }
            }
        }

        public String getLastNameError() {
            try {
                return getTextFromElement(inputErrorLastName);
            } catch (NoSuchElementException e) {
                try {
                    return getTextFromElement(inputErrorLastNameDynamic);
                } catch (NoSuchElementException ex) {
                    return null;
                }
            }
        }

        public String getEmailError() {
            try {
                return getTextFromElement(inputErrorEmail);
            } catch (NoSuchElementException e) {
                return null;
            }
        }

        public String getUsernameError() {
            try {
                return getTextFromElement(inputErrorUsername);
            } catch (NoSuchElementException e) {
                return null;
            }
        }
    }
}
