package org.keycloak.testsuite.admin.page.settings.user;

import org.keycloak.testsuite.admin.page.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.admin.util.SeleniumUtils.waitGuiForElement;

/**
 * Created by fkiss.
 */
public class RoleMappingsPage extends AbstractPage {

    @FindBy(id = "available")
    private Select availableRolesSelect;

    @FindBy(id = "assigned")
    private Select assignedRolesSelect;

    @FindBy(id = "realm-composite")
    private Select effectiveRolesSelect;

    @FindBy(id = "available-client")
    private Select availableClientRolesSelect;

    @FindBy(id = "assigned-client")
    private Select assignedClientRolesSelect;

    @FindBy(css = "button[ng-click*='addRealm']")
    private WebElement addSelected;

    @FindBy(css = "button[ng-click*='addRealm']")
    private WebElement addSelectedButton;

    @FindBy(css = "button[ng-click*='deleteRealm']")
    private WebElement removeSelectedButton;

    @FindBy(id = "clients")
    private Select clientRolesSelect;

    public void addAvailableRole(String role){
        waitGuiForElement(By.id("available"));
        availableRolesSelect.selectByVisibleText(role);
        addSelected.click();
    }

    public void removeAssignedRole(String client){
        waitGuiForElement(By.id("assigned"));
        assignedRolesSelect.selectByVisibleText(client);
        removeSelectedButton.click();
    }

    public void selectClientRole(String client){
        waitGuiForElement(By.id("clients"));
        clientRolesSelect.selectByVisibleText(client);
    }

    public void addAvailableClientRole(String role){
        waitGuiForElement(By.id("available-client"));
        availableRolesSelect.selectByVisibleText(role);
        addSelected.click();
    }

    public void removeAssignedClientRole(String client){
        waitGuiForElement(By.id("assigned-client"));
        assignedClientRolesSelect.selectByVisibleText(client);
        removeSelectedButton.click();
    }
}
