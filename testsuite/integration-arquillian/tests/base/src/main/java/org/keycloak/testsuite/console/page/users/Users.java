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
package org.keycloak.testsuite.console.page.users;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;
import org.keycloak.representations.idm.UserRepresentation;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import static org.keycloak.testsuite.util.SeleniumUtils.waitAjaxForElement;
import static org.openqa.selenium.By.*;

/**
 *
 * @author Filip Kiss
 * @author tkyjovsk
 */
public class Users extends AdminConsoleRealm {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/users";
    }

    public static final String VIEW_ALL_USERS = "View all users";
    public static final String UNLOCK_USERS = "Unlock Users";
    public static final String ADD_USER = "Add User";

    public static final String EDIT = "Edit";
    public static final String IMPERSONATE = "Impersonate";
    public static final String DELETE = "Delete";

    @FindBy(css = "table[class*='table']")
    private DataTable table;

    public List<UserRepresentation> searchUsers(String searchPattern) {
        table.search(searchPattern);
        return getUsersFromTable();
    }

    public void viewAllUsers() {
        table.clickHeaderButton(VIEW_ALL_USERS);
    }

    public void unlockUsers() {
        table.clickHeaderButton(UNLOCK_USERS);
        // FIXME verify notification
    }

    public void clickUser(String username) {
        waitAjaxForElement(table.body());
        table.body().findElement(linkText(username)).click();
    }

    public void editUser(String username) {
        table.clickActionButton(table.getRowByLinkText(username), EDIT);
    }

    public void impersonateUser(String username) {
        table.clickActionButton(table.getRowByLinkText(username), IMPERSONATE);
    }

    public void deleteUser(String username) {
        table.clickActionButton(table.getRowByLinkText(username), DELETE);
        // FIXME verify notification
    }

    public void addUser() {
        table.clickHeaderButton(ADD_USER);
    }

    public UserRepresentation findUser(String searchPattern) {
        List<UserRepresentation> users = searchUsers(searchPattern);
        if (users.isEmpty()) {
            return null;
        } else {
            assert 1 == users.size();
            return users.get(0);
        }
    }

    private List<UserRepresentation> getUsersFromTable() {
        List<UserRepresentation> users = new ArrayList<>();
        List<WebElement> rows = table.rows();
        if (rows.size() > 1) {
            for (WebElement rowElement : rows) {
                if (rowElement.isDisplayed()) {
                    UserRepresentation user = new UserRepresentation();
                    List<WebElement> tds = rowElement.findElements(tagName("td"));
                    if (!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
                        user.setUsername(tds.get(0).getText());
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
