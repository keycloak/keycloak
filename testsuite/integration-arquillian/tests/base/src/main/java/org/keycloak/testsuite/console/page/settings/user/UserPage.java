/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.page.settings.user;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.model.User;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.testsuite.model.RequiredUserAction;
import org.keycloak.testsuite.console.page.Realm;
import static org.keycloak.testsuite.util.SeleniumUtils.waitAjaxForElement;
import static org.openqa.selenium.By.*;

/**
 *
 * @author Filip Kiss
 */
public class UserPage extends Realm {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "emailVerified")
    private WebElement emailVerifiedSwitchToggle;

    @FindBy(css = "label[for='userEnabled']")
    private WebElement userEnabledSwitchToggle;

    @FindBy(css = "input[id='s2id_autogen3']")
    private WebElement requiredUserActionsInput;

    @FindBy(className = "select2-result-label")
    private WebElement requiredUserActionsConfirm;

    @FindBy(className = "select2-search-choice-close")
    private List<WebElement> removeRequiredActionsList;

    @FindBy(id = "password")
    private WebElement password;

    @FindBy(id = "confirmPassword")
    private WebElement confirmPassword;

    @FindBy(css = "input[class*='search']")
    private WebElement searchInput;

    @FindBy(css = "table[class*='table']")
    private WebElement dataTable;

    @FindBy(css = "tr[ng-repeat='user in users']")
    private WebElement tableRow;

    @FindByJQuery("button[kc-cancel] ")
    private WebElement cancel;

    @FindBy(css = "div[class='input-group-addon'] i")
    private WebElement searchButton;

    @FindBy(id = "createUser")
    private WebElement addUserButton;

    @FindBy(id = "removeUser")
    private WebElement removeUserButton;

    @Override
    public String getFragment() {
        return super.getFragment() + "/users";
    }

    public void addUser(User user) {
        addUserButton.click();
        waitAjaxForElement(usernameInput);
        usernameInput.sendKeys(user.getUserName());
        emailInput.sendKeys(user.getEmail());
        firstNameInput.sendKeys(user.getFirstName());
        lastNameInput.sendKeys(user.getLastName());
        if (!user.isUserEnabled()) {
            userEnabledSwitchToggle.click();
        }
        if (user.isEmailVerified()) {
            emailVerifiedSwitchToggle.click();
        }
        if(!user.getRequiredUserActions().isEmpty()){
            for (RequiredUserAction action : user.getRequiredUserActions()) {
                //driver.findElement(By.cssSelector("..select2-choices")).click();
                requiredUserActionsInput.sendKeys(action.getActionName());
                requiredUserActionsConfirm.click();
            }
        }
        primaryButton.click();
        }



    public void addPasswordForUser(User user) {
        password.sendKeys(user.getPassword());
        confirmPassword.sendKeys(user.getPassword());
        dangerButton.click();
        waitAjaxForElement(deleteConfirmationButton);
        deleteConfirmationButton.click();
    }

    public User findUser(String username) {
        waitAjaxForElement(searchInput);
        searchInput.sendKeys(username);
        searchButton.click();
        List<User> users = getAllRows();
        if (users.isEmpty()) {
            return null;

        } else {
            assert 1 == users.size();
            return users.get(0);
        }
    }

    public void updateUser(User user) {
        goToUser(user);
        waitAjaxForElement(usernameInput);
        usernameInput.sendKeys(user.getUserName());
        emailInput.clear();
        emailInput.sendKeys(user.getEmail());
        if (!user.isUserEnabled()) {
            userEnabledSwitchToggle.click();
        }
        if (user.isEmailVerified()) {
            emailVerifiedSwitchToggle.click();
        }
        if (user.getRequiredUserActions().isEmpty()) {
            for (WebElement e : removeRequiredActionsList) {
                e.click();
            }
        } else {
            for (RequiredUserAction action : user.getRequiredUserActions()) {
                requiredUserActionsInput.sendKeys(action.getActionName());
                requiredUserActionsConfirm.click();
            }
        }
        primaryButtons.get(1).click();
    }

    public void deleteUser(String username) {
        findUser(username);
        goToUser(username);
        waitAjaxForElement(removeUserButton);
        removeUserButton.click();
        waitAjaxForElement(deleteConfirmationButton);
        deleteConfirmationButton.click();
    }

    public void cancel() {
        cancel.click();
    }

    public void showAllUsers() {
        primaryButtons.get(0).click();
    }

    public void goToUser(User user) {
        waitAjaxForElement(tableRow);
        dataTable.findElement(linkText(user.getUserName())).click();
    }

    public void goToUser(String name) {
        goToUser(new User(name));
    }

    private List<User> getAllRows() {
        List<User> users = new ArrayList<>();
        List<WebElement> rows = dataTable.findElements(cssSelector("tbody tr"));
        if (rows.size() > 1) {
            for (WebElement rowElement : rows) {
                if (rowElement.isDisplayed()) {
                    User user = new User();
                    List<WebElement> tds = rowElement.findElements(tagName("td"));
                    if (!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
                        user.setUserName(tds.get(0).getText());
                        user.setLastName(tds.get(1).getText());
                        user.setFirstName(tds.get(2).getText());
                        user.setEmail(tds.get(3).getText());
                        users.add(user);
                    }
                }
            }
        }
        return users;
    }

}
