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
package org.keycloak.testsuite.console.page.roles;

import org.keycloak.testsuite.console.page.users.UserRoleMappingsForm;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.RoleRepresentation;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.linkText;
import static org.openqa.selenium.By.tagName;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import static org.keycloak.testsuite.util.SeleniumUtils.*;

/**
 *
 * @author Petr Mensik
 */
public class RealmRoles extends Roles {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/roles";
    }

    @Page
    private UserRoleMappingsForm roleMappings;

    @FindBy(css = "input[class*='search']")
    private WebElement searchInput;

    @FindBy(css = "table[class*='table']")
    private WebElement dataTable;

    @FindBy(id = "name")
    private WebElement nameInput;

    @FindBy(id = "description")
    private WebElement descriptionInput;

    @FindBy(className = "onoffswitch-switch")
    private WebElement compositeSwitchToggle;

    @FindByJQuery("a:contains('Add Role')")
    private WebElement addRoleButton;

    @FindBy(id = "removeRole")
    private WebElement removeRoleButton;

    public boolean isRoleComposite(String roleName) {
        return findRole(roleName).isComposite();
    }

    public void addRole(RoleRepresentation role) {
        addRoleButton.click();
        waitAjaxForElement(nameInput);
        nameInput.sendKeys(role.getName());
        descriptionInput.sendKeys(role.getDescription());
        primaryButton.click();
    }

    public void addRole(RoleRepresentation role, String... roles) {
        addRole(role);
        setCompositeRole(role, roles);
    }

    public RoleRepresentation findRole(String name) {
        searchInput.sendKeys(name);
        List<RoleRepresentation> roles = getAllRows();
        assert 1 == roles.size();
        return roles.get(0);
    }

    public void editRole(RoleRepresentation role) {
        driver.findElement(linkText(role.getName())).click();
        waitAjaxForElement(nameInput);
        nameInput.sendKeys(role.getName());
        if (role.isComposite()) {
            compositeSwitchToggle.click();
        }
        descriptionInput.sendKeys(role.getDescription());
        primaryButton.click();
    }

    public void goToRole(RoleRepresentation role) {
        goToRole(role.getName());
    }

    public void goToRole(String name) {
        waitAjaxForElement(dataTable);
        dataTable.findElement(linkText(name)).click();
    }

    public void deleteRole(RoleRepresentation role) {
        deleteRole(role.getName());
    }

    public void deleteRole(String name) {
        driver.findElement(linkText(name)).click();
        waitAjaxForElement(removeRoleButton);
        removeRoleButton.click();
        deleteConfirmationButton.click();
    }

    public void setCompositeRole(RoleRepresentation role, String... roles) {
        if (role.isComposite()) {
            waitAjaxForElement(compositeSwitchToggle);
            compositeSwitchToggle.click();
            roleMappings.addAvailableRole(roles);
        }
    }

    private List<RoleRepresentation> getAllRows() {
        List<RoleRepresentation> rows = new ArrayList<>();
        for (WebElement rowElement : dataTable.findElements(cssSelector("tbody tr"))) {
            RoleRepresentation role = new RoleRepresentation();
            List<WebElement> tds = rowElement.findElements(tagName("td"));
            if (!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
                role.setName(tds.get(0).getText());
                role.setComposite(Boolean.valueOf(tds.get(1).getText()));
                role.setDescription(tds.get(2).getText());
                rows.add(role);
            }
        }
        return rows;
    }
}
