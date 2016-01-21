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
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;
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

    @FindBy(xpath = "//div[./h1[text()='Users']]/table")
    private UsersTable table;

    public UsersTable table() {
        return table;
    }

    public class UsersTable extends DataTable {

        public List<UserRepresentation> searchUsers(String searchPattern) {
            search(searchPattern);
            return getUsersFromTableRows();
        }

        public void viewAllUsers() {
            clickHeaderButton(VIEW_ALL_USERS);
        }

        public void unlockUsers() {
            clickHeaderButton(UNLOCK_USERS);
        }

        public void clickUser(String username) {
            waitUntilElement(body()).is().present();
            body().findElement(linkText(username)).click();
        }

        public void editUser(String username) {
            clickRowActionButton(getRowByLinkText(username), EDIT);
        }

        public void impersonateUser(String username) {
            clickRowActionButton(getRowByLinkText(username), IMPERSONATE);
        }

        public void deleteUser(String username) {
            clickRowActionButton(getRowByLinkText(username), DELETE);
            modalDialog.confirmDeletion();
        }

        public void addUser() {
            clickHeaderLink(ADD_USER);
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

        public UserRepresentation getUserFromTableRow(WebElement row) {
            UserRepresentation user = null;
            List<WebElement> tds = row.findElements(tagName("td"));
            if (!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
                user = new UserRepresentation();
                user.setUsername(tds.get(0).getText());
                user.setLastName(tds.get(1).getText());
                user.setFirstName(tds.get(2).getText());
                user.setEmail(tds.get(3).getText());
            }
            return user;
        }

        public List<UserRepresentation> getUsersFromTableRows() {
            List<UserRepresentation> users = new ArrayList<>();
            List<WebElement> rows = rows();
//            if (rows.size() > 1) {
                for (WebElement rowElement : rows) {
                    if (rowElement.isDisplayed()) {
                        UserRepresentation user = getUserFromTableRow(rowElement);
                        if (user != null) {
                            users.add(user);
                        }
                    }
                }
//            }
            return users;
        }

    }
    
    public UsersResource usersResource() {
        return realmResource().users();
    }

}
