/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.console.page.roles;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.openqa.selenium.By.tagName;
import static org.openqa.selenium.By.xpath;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class UsersInRole extends Role {
    @FindBy(css = "table[class*='table']")
    private UsersInRoleTable usersInRoleTable;

    public UsersInRoleTable usersTable() {
        return usersInRoleTable;
    }

    public class UsersInRoleTable extends DataTable {

        public void clickUser(String userName) {
            clickRowByLinkText(userName);
        }

        public void editUser(String userName) {
            clickRowActionButton(getRowByLinkText(userName), "Edit");
        }

        public UserRepresentation getUserFromTableRow(WebElement row) {
            UserRepresentation user = null;
            List<WebElement> tds = row.findElements(tagName("td"));
            if (tds.size() == 5 && !getTextFromElement(tds.get(0)).isEmpty()) {
                user = new UserRepresentation();
                user.setUsername(getTextFromElement(tds.get(0)));
                user.setLastName(getTextFromElement(tds.get(1)));
                user.setFirstName(getTextFromElement(tds.get(2)));
                user.setEmail(getTextFromElement(tds.get(3)));
            }
            return user;
        }

        public List<UserRepresentation> getUsersFromTableRows() {
            return rows().stream()
                    .map(this::getUserFromTableRow)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        public boolean noRoleMembersIsDisplayed() {
            try {
                return body().findElement(xpath(".//td[text()='No role members' and not(contains(@class, 'ng-hide'))]")).isDisplayed();
            }
            catch (NoSuchElementException e) {
                return false;
            }
        }

    }
}
