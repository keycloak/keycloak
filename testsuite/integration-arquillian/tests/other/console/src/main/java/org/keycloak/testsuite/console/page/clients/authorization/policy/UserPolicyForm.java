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
package org.keycloak.testsuite.console.page.clients.authorization.policy;

import static org.openqa.selenium.By.tagName;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.arquillian.graphene.fragment.Root;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserPolicyForm extends Form {

    @FindBy(id = "name")
    private WebElement name;

    @FindBy(id = "description")
    private WebElement description;

    @FindBy(id = "logic")
    private Select logic;

    @FindBy(xpath = "//i[contains(@class,'pficon-delete')]")
    private WebElement deleteButton;

    @FindBy(id = "s2id_users")
    private UsersInput usersInput;

    @FindBy(xpath = ACTIVE_DIV_XPATH + "/button[text()='Delete']")
    private WebElement confirmDelete;

    public void populate(UserPolicyRepresentation expected) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        logic.selectByValue(expected.getLogic().name());

        Set<String> users = expected.getUsers();

        for (String user : users) {
            usersInput.select(user, driver);
        }

        unSelect(users, usersInput.getSelected());

        save();
    }

    private void unSelect(Set<String> users, Set<String> selection) {
        for (String selected : selection) {
            boolean isSelected = false;

            for (String user : users) {
                if (selected.equals(user)) {
                    isSelected = true;
                    break;
                }
            }

            if (!isSelected) {
                usersInput.unSelect(selected, driver);
            }
        }
    }

    public void delete() {
        deleteButton.click();
        confirmDelete.click();
    }

    public UserPolicyRepresentation toRepresentation() {
        UserPolicyRepresentation representation = new UserPolicyRepresentation();

        representation.setName(getInputValue(name));
        representation.setDescription(getInputValue(description));
        representation.setLogic(Logic.valueOf(logic.getFirstSelectedOption().getText().toUpperCase()));
        representation.setUsers(usersInput.getSelected());

        return representation;
    }

    public class UsersInput extends AbstractUserInput {
        @Override
        protected WebElement getRemoveButton(List<WebElement> tds) {
            return tds.get(1);
        }

        @Override
        protected List<WebElement> getSelectedElements() {
            return root.findElements(By.xpath("(//table[@id='selected-users'])/tbody/tr"));
        }
    }

    public abstract class AbstractUserInput {

        @Root
        protected WebElement root;

        @FindBy(xpath = "//div[contains(@class,'select2-result-label')]")
        private List<WebElement> result;

        @FindBy(xpath = "//li[contains(@class,'select2-search-choice')]")
        private List<WebElement> selection;

        public void select(String roleId, WebDriver driver) {
            root.click();
            WaitUtils.pause(1000);

            Actions actions = new Actions(driver);

            actions.sendKeys(roleId).perform();
            WaitUtils.pause(1000);

            if (result.isEmpty()) {
                actions.sendKeys(Keys.ESCAPE).perform();
                return;
            }

            for (WebElement result : result) {
                if (result.getText().equalsIgnoreCase(roleId)) {
                    result.click();
                    return;
                }
            }
        }

        public Set<String> getSelected() {
            List<WebElement> users = getSelectedElements();
            Set<String> values = new HashSet<>();

            for (WebElement user : users) {
                List<WebElement> tds = user.findElements(tagName("td"));
                if (!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
                    values.add(tds.get(0).getText());
                }
            }

            return values;
        }

        protected abstract List<WebElement> getSelectedElements();

        public void unSelect(String name, WebDriver driver) {
            Iterator<WebElement> iterator = getSelectedElements().iterator();

            while (iterator.hasNext()) {
                WebElement realmRole = iterator.next();
                List<WebElement> tds = realmRole.findElements(tagName("td"));

                if (!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
                    if (tds.get(0).getText().equals(name)) {
                        getRemoveButton(tds).findElement(By.tagName("button")).click();
                        return;
                    }
                }
            }
        }

        protected abstract WebElement getRemoveButton(List<WebElement> tds);
    }
}