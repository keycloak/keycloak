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

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginUpdateProfileEditUsernameAllowedPage extends LoginUpdateProfilePage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    public void update(String firstName, String lastName, String email, String username) {
        usernameInput.clear();
        usernameInput.sendKeys(username);
        update(firstName, lastName, email);
    }

    public void updateWithDepartment(String firstName, String lastName, String department, String email, String username) {
        usernameInput.clear();
        usernameInput.sendKeys(username);
        super.updateWithDepartment(firstName, lastName, department, email);
    }

    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).equals("Update Account Information");
    }
    
    public boolean isUsernamePresent() {
        try {
            return driver.findElement(By.id("username")).isDisplayed();
        } catch (NoSuchElementException nse) {
            return false;
        }
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }

}
