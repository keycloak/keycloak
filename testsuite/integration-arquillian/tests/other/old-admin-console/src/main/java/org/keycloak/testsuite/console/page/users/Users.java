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

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.openqa.selenium.By.tagName;

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
    public static final String UNLOCK_USERS = "Unlock users";
    public static final String ADD_USER = "Add user";

    public static final String EDIT = "Edit";
    public static final String IMPERSONATE = "Impersonate";
    public static final String DELETE = "Delete";

    @FindBy(id = "user-table")
    private UsersTable table;

    public UsersTable table() {
        return table;
    }

    public class UsersTable extends DataTable {

        @Drone
        private WebDriver driver;

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
            clickLink(getRowByUsername(username).findElement(By.xpath("./td[position()=1]")));
        }

        public void editUser(String username) {
            clickRowActionButton(getRowByUsername(username), EDIT);
        }

        public void impersonateUser(String username) {
            clickRowActionButton(getRowByUsername(username), IMPERSONATE);
        }

        public void deleteUser(String username) {
            clickRowActionButton(getRowByUsername(username), DELETE);
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
            if (!(tds.isEmpty() || getTextFromElement(tds.get(0)).isEmpty())) {
                user = new UserRepresentation();
                user.setUsername(getTextFromElement(tds.get(0)));
                user.setLastName(getTextFromElement(tds.get(1)));
                user.setFirstName(getTextFromElement(tds.get(2)));
                user.setEmail(getTextFromElement(tds.get(3)));
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

        protected WebElement getRowByUsername(String userName) {
            return body().findElement(
                    By.xpath(".//tr[./td[position()=2 and text()='" + userName + "']]")
            );
        }

    }
    
}
