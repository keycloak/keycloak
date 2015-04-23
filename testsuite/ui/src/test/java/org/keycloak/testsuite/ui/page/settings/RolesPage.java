/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.page.settings;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.keycloak.testsuite.ui.page.AbstractPage;
import org.keycloak.testsuite.ui.model.Role;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.linkText;
import static org.openqa.selenium.By.tagName;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.*;
/**
 *
 * @author pmensik
 */
public class RolesPage extends AbstractPage {

    @FindBy(css = "input[class*='search']")
    private WebElement searchInput;

    @FindBy(css = "table[class*='table']")
    private WebElement dataTable;

    @FindBy(id = "name")
    private WebElement nameInput;

    @FindBy(id = "description")
    private WebElement descriptionInput;

    @FindBy(id = "compositeSwitch")
    private WebElement compositeSwitchToggle;
	

    public boolean isRoleComposite(String roleName) {
        return findRole(roleName).isComposite();
    }

    public void addRole(Role role) {
		primaryButton.click();
		waitAjaxForElement(nameInput);
        nameInput.sendKeys(role.getName());
        if (role.isComposite()) {
            compositeSwitchToggle.click();
        }
        descriptionInput.sendKeys(role.getDescription());
		primaryButton.click();
	}

    public Role findRole(String roleName) {
        searchInput.sendKeys(roleName);
        List<Role> roles = getAllRows();
        assertEquals(1, roles.size());
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
        waitAjaxForElement(dangerButton);
		dangerButton.click();
		deleteConfirmationButton.click();
    }
	
	public void deleteRole(String name) {
		deleteRole(new Role(name));
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
