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
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
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
public class RolePolicyForm extends Form {

    @FindBy(id = "name")
    private WebElement name;

    @FindBy(id = "description")
    private WebElement description;

    @FindBy(id = "logic")
    private Select logic;

    @FindBy(xpath = "//i[contains(@class,'pficon-delete')]")
    private WebElement deleteButton;

    @FindBy(id = "s2id_roles")
    private RolesInput realmRolesInput;

    @FindBy(id = "clients")
    private Select clientsSelect;

    @FindBy(id = "s2id_clientRoles")
    private ClientRolesInput clientRolesInput;

    @FindBy(xpath = ACTIVE_DIV_XPATH + "/button[text()='Delete']")
    private WebElement confirmDelete;

    public void populate(RolePolicyRepresentation expected) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        logic.selectByValue(expected.getLogic().name());

        Set<RolePolicyRepresentation.RoleDefinition> roles = expected.getRoles();

        for (RolePolicyRepresentation.RoleDefinition role : roles) {
            boolean clientRole = role.getId().indexOf('/') != -1;

            if (clientRole) {
                String[] parts = role.getId().split("/");
                clientsSelect.selectByVisibleText(parts[0]);
                clientRolesInput.select(parts[1], driver);
                clientRolesInput.setRequired(parts[1], role);
            } else {
                realmRolesInput.select(role.getId(), driver);
                realmRolesInput.setRequired(role.getId(), role);
            }
        }

        unSelect(roles, realmRolesInput.getSelected());
        unSelect(roles, clientRolesInput.getSelected());

        save();
    }

    private void unSelect(Set<RolePolicyRepresentation.RoleDefinition> roles, Set<RolePolicyRepresentation.RoleDefinition> selection) {
        for (RolePolicyRepresentation.RoleDefinition selected : selection) {
            boolean isSelected = false;

            for (RolePolicyRepresentation.RoleDefinition scope : roles) {
                if (selected.getId().equals(scope.getId())) {
                    isSelected = true;
                    break;
                }
            }

            if (!isSelected) {
                boolean clientRole = selected.getId().indexOf('/') != -1;

                if (clientRole) {
                    clientRolesInput.unSelect(selected.getId().split("/")[1], driver);
                } else {
                    realmRolesInput.unSelect(selected.getId(), driver);
                }
            }
        }
    }

    public void delete() {
        deleteButton.click();
        confirmDelete.click();
    }

    public RolePolicyRepresentation toRepresentation() {
        RolePolicyRepresentation representation = new RolePolicyRepresentation();

        representation.setName(getInputValue(name));
        representation.setDescription(getInputValue(description));
        representation.setLogic(Logic.valueOf(logic.getFirstSelectedOption().getText().toUpperCase()));

        Set<RolePolicyRepresentation.RoleDefinition> roles = realmRolesInput.getSelected();

        roles.addAll(clientRolesInput.getSelected());

        representation.setRoles(roles);

        return representation;
    }

    public class RolesInput extends AbstractRolesInput {
        @Override
        protected RolePolicyRepresentation.RoleDefinition getSelectedRoles(List<WebElement> tds) {
            RolePolicyRepresentation.RoleDefinition selectedRole = new RolePolicyRepresentation.RoleDefinition();
            selectedRole.setId(tds.get(0).getText());
            selectedRole.setRequired(tds.get(1).findElement(By.tagName("input")).isSelected());
            return selectedRole;
        }

        @Override
        protected WebElement getRemoveButton(List<WebElement> tds) {
            return tds.get(2);
        }

        @Override
        protected List<WebElement> getSelectedElements() {
            return root.findElements(By.xpath("(//table[@id='selected-realm-roles'])/tbody/tr"));
        }

        @Override
        protected WebElement getRequiredColumn(List<WebElement> tds) {
            return tds.get(1);
        }
    }

    public class ClientRolesInput extends AbstractRolesInput {
        @Override
        protected WebElement getRemoveButton(List<WebElement> tds) {
            return tds.get(3);
        }

        @Override
        protected RolePolicyRepresentation.RoleDefinition getSelectedRoles(List<WebElement> tds) {
            RolePolicyRepresentation.RoleDefinition selectedRole = new RolePolicyRepresentation.RoleDefinition();
            selectedRole.setId(tds.get(1).getText() + "/" + tds.get(0).getText());
            selectedRole.setRequired(tds.get(2).findElement(By.tagName("input")).isSelected());
            return selectedRole;
        }

        @Override
        protected List<WebElement> getSelectedElements() {
            return root.findElements(By.xpath("(//table[@id='selected-client-roles'])/tbody/tr"));
        }

        @Override
        protected WebElement getRequiredColumn(List<WebElement> tds) {
            return tds.get(2);
        }
    }

    public abstract class AbstractRolesInput {

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

        public Set<RolePolicyRepresentation.RoleDefinition> getSelected() {
            List<WebElement> realmRoles = getSelectedElements();
            Set<RolePolicyRepresentation.RoleDefinition> values = new HashSet<>();

            for (WebElement realmRole : realmRoles) {
                List<WebElement> tds = realmRole.findElements(tagName("td"));
                if (!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
                    values.add(getSelectedRoles(tds));
                }
            }

            return values;
        }

        protected abstract RolePolicyRepresentation.RoleDefinition getSelectedRoles(List<WebElement> tds);

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

        public void setRequired(String name, RolePolicyRepresentation.RoleDefinition role) {
            Iterator<WebElement> iterator = getSelectedElements().iterator();

            while (iterator.hasNext()) {
                WebElement realmRole = iterator.next();
                List<WebElement> tds = realmRole.findElements(tagName("td"));

                if (!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
                    if (tds.get(0).getText().equals(name)) {
                        WebElement required = getRequiredColumn(tds).findElement(By.tagName("input"));

                        if (required.isSelected() && role.isRequired()) {
                            return;
                        } else if (!required.isSelected() && !role.isRequired()) {
                            return;
                        }

                        required.click();
                        return;
                    }
                }
            }
        }

        protected abstract WebElement getRequiredColumn(List<WebElement> tds);
    }
}