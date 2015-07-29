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

package org.keycloak.testsuite.console.page.settings;

import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.fragment.RoleMappings;
import org.keycloak.testsuite.console.page.AdminConsole;
import org.keycloak.testsuite.model.Role;
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
public class RolesPage extends AdminConsole {

    @Page
    private RoleMappings roleMappings;

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

    public void addRole(Role role) {
        addRoleButton.click();
		waitAjaxForElement(nameInput);
        nameInput.sendKeys(role.getName());
        descriptionInput.sendKeys(role.getDescription());
		primaryButton.click();
	}

    public void addRole(Role role, String... roles) {
        addRole(role);
        setCompositeRole(role, roles);
    }

    public Role findRole(String roleName) {
        searchInput.sendKeys(roleName);
        List<Role> roles = getAllRows();
        assert 1 ==roles.size();
        return roles.get(0);
    }

    public void editRole(Role role) {
        driver.findElement(linkText(role.getName())).click();
        waitAjaxForElement(nameInput);
        nameInput.sendKeys(role.getName());
        if (role.isComposite()) {
            compositeSwitchToggle.click();
        }
        descriptionInput.sendKeys(role.getDescription());
		primaryButton.click();
	}

    public void deleteRole(Role role) {
        driver.findElement(linkText(role.getName())).click();
        waitAjaxForElement(removeRoleButton);
        removeRoleButton.click();
		deleteConfirmationButton.click();
    }

    public void goToRole(Role role) {
        waitAjaxForElement(dataTable);
        dataTable.findElement(linkText(role.getName())).click();
    }

    public void goToRole(String name) {
        goToRole(new Role(name));
    }
	
	public void deleteRole(String name) {
		deleteRole(new Role(name));
	}

    public void setCompositeRole(Role role, String... roles){
        if (role.isComposite()) {
            waitAjaxForElement(compositeSwitchToggle);
            compositeSwitchToggle.click();
            roleMappings.addAvailableRole(roles);
        }
    }

    private List<Role> getAllRows() {
        List<Role> rows = new ArrayList<Role>();
        for (WebElement rowElement : dataTable.findElements(cssSelector("tbody tr"))) {
            Role role = new Role();
            List<WebElement> tds = rowElement.findElements(tagName("td"));
            if(!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
                role.setName(tds.get(0).getText());
                role.setComposite(Boolean.valueOf(tds.get(1).getText()));
                role.setDescription(tds.get(2).getText());
                rows.add(role);
            }
        }
        return rows;
    }
}
