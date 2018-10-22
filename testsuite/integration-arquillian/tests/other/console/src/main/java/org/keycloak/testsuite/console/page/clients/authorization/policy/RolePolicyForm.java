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

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.openqa.selenium.By.tagName;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.testsuite.console.page.fragment.AbstractMultipleSelect2;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
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
    private RoleMultipleSelect2 realmRoleSelect;

    @FindBy(id = "clients")
    private Select clientsSelect;

    @FindBy(id = "s2id_clientRoles")
    private ClientRoleSelect clientRoleSelect;

    @FindBy(xpath = "//div[@class='modal-dialog']")
    protected ModalDialog modalDialog;

    public void populate(RolePolicyRepresentation expected, boolean save) {
        UIUtils.setTextInputValue(name, expected.getName());
        UIUtils.setTextInputValue(description, expected.getDescription());
        logic.selectByValue(expected.getLogic().name());

        Set<RolePolicyRepresentation.RoleDefinition> roles = expected.getRoles();

        for (RolePolicyRepresentation.RoleDefinition role : roles) {
            boolean clientRole = role.getId().indexOf('/') != -1;

            if (clientRole) {
                String[] parts = role.getId().split("/");
                clientsSelect.selectByVisibleText(parts[0]);
                clientRoleSelect.select(role);
                clientRoleSelect.setRequired(role);
            } else {
                realmRoleSelect.select(role);
                realmRoleSelect.setRequired(role);
            }
        }

        unSelect(roles, realmRoleSelect.getSelected());
        unSelect(roles, clientRoleSelect.getSelected());

        if (save) {
            save();
        }
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
                    clientRoleSelect.deselect(selected);
                } else {
                    realmRoleSelect.deselect(selected);
                }
            }
        }
    }

    public void delete() {
        deleteButton.click();
        modalDialog.confirmDeletion();
    }

    public RolePolicyRepresentation toRepresentation() {
        RolePolicyRepresentation representation = new RolePolicyRepresentation();

        representation.setName(UIUtils.getTextInputValue(name));
        representation.setDescription(UIUtils.getTextInputValue(description));
        representation.setLogic(Logic.valueOf(UIUtils.getTextFromElement(logic.getFirstSelectedOption()).toUpperCase()));

        Set<RolePolicyRepresentation.RoleDefinition> roles = realmRoleSelect.getSelected();

        roles.addAll(clientRoleSelect.getSelected());

        representation.setRoles(roles);

        return representation;
    }

    public class RoleMultipleSelect2 extends AbstractMultipleSelect2<RolePolicyRepresentation.RoleDefinition> {

        @Override
        protected Function<RolePolicyRepresentation.RoleDefinition, String> identity() {
            return role -> {
                String id = role.getId();
                return id.indexOf('/') != -1 ? id.split("/")[1] : id;
            };
        }

        @Override
        protected List<WebElement> getSelectedElements() {
            return getRoot().findElements(By.xpath("(//table[@id='selected-realm-roles'])/tbody/tr")).stream()
                    .filter(webElement -> webElement.findElements(tagName("td")).size() > 1)
                    .collect(Collectors.toList());
        }

        @Override
        protected Function<WebElement, RolePolicyRepresentation.RoleDefinition> representation() {
            return webElement -> {
                List<WebElement> tds = webElement.findElements(tagName("td"));
                RolePolicyRepresentation.RoleDefinition selectedRole = new RolePolicyRepresentation.RoleDefinition();
                boolean clientRole = tds.size() == 4;

                selectedRole.setId(clientRole ? getTextFromElement(tds.get(1)) + "/" + getTextFromElement(tds.get(0)) : getTextFromElement(tds.get(0)));
                selectedRole.setRequired(tds.get(clientRole ? 2 : 1).findElement(By.tagName("input")).isSelected());

                return selectedRole;
            };
        }

        public void setRequired(RolePolicyRepresentation.RoleDefinition role) {
            Iterator<WebElement> iterator = getSelectedElements().iterator();

            while (iterator.hasNext()) {
                WebElement realmRole = iterator.next();
                List<WebElement> tds = realmRole.findElements(tagName("td"));
                boolean clientRole = role.getId().indexOf("/") != -1;
                WebElement roleName = tds.get(0);

                if (!UIUtils.getTextFromElement(roleName).isEmpty()) {
                    if (UIUtils.getTextFromElement(roleName).equals(getRoleId(role, clientRole))) {
                        WebElement required = tds.get(clientRole ? 2 : 1).findElement(By.tagName("input"));

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

        @Override
        protected BiFunction<WebElement, RolePolicyRepresentation.RoleDefinition, Boolean> deselect() {
            return (webElement, roleDefinition) -> {
                List<WebElement> tds = webElement.findElements(tagName("td"));
                boolean clientRole = tds.size() == 4;

                if (!UIUtils.getTextFromElement(tds.get(0)).isEmpty()) {
                    if (UIUtils.getTextFromElement(tds.get(0)).equals(getRoleId(roleDefinition, clientRole))) {
                        tds.get(clientRole ? 3 : 2).findElement(By.tagName("button")).click();
                        return true;
                    }
                }

                return false;
            };
        }

        private String getRoleId(RolePolicyRepresentation.RoleDefinition roleDefinition, boolean clientRole) {
            return clientRole ? roleDefinition.getId().split("/")[1] : roleDefinition.getId();
        }
    }

    public class ClientRoleSelect extends RoleMultipleSelect2 {
        @Override
        protected List<WebElement> getSelectedElements() {
            return getRoot().findElements(By.xpath("(//table[@id='selected-client-roles'])/tbody/tr")).stream()
                    .filter(webElement -> webElement.findElements(tagName("td")).size() > 1)
                    .collect(Collectors.toList());
        }
    }
}