package org.keycloak.testsuite.ui.fragment;

import org.keycloak.testsuite.ui.model.Role;
import org.keycloak.testsuite.ui.page.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;

/**
 * Created by fkiss.
 */
public class RoleMappings extends AbstractPage {

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

    public void removeAssignedRole(String role){
        waitGuiForElement(By.id("assigned"));
        assignedRolesSelect.selectByVisibleText(role);
        removeSelectedButton.click();
    }

    public boolean isAssignedRole(String role){
        waitGuiForElement(By.id("assigned"));
        try {
            assignedRolesSelect.selectByVisibleText(role);
        } catch (Exception ex){
            return false;
        }
        return true;
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

    public void addAvailableRole(String... roles){
        waitGuiForElement(By.id("available"));
        for(String role : roles) {
            availableRolesSelect.selectByVisibleText(role);
            addSelected.click();
        }
    }

    public boolean checkIfEffectiveRolesAreComplete(List<Role> roles){
        List<String> roleNames = new ArrayList<>();
        for (Role role : roles){
            roleNames.add(role.getName());
        }
        for (WebElement role : effectiveRolesSelect.getOptions()){
            roleNames.contains(role.getText());
            roleNames.remove(role.getText());
        }
        return roleNames.isEmpty();
    }
}
