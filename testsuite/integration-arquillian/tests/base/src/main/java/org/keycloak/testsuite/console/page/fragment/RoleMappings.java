package org.keycloak.testsuite.console.page.fragment;

import org.keycloak.testsuite.model.Role;
import org.keycloak.testsuite.console.page.AdminConsole;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.util.SeleniumUtils.waitGuiForElement;

/**
 * Created by fkiss.
 */
public class RoleMappings extends AdminConsole {

    @FindBy(id = "available")
    private Select availableRolesSelect;

    @FindBy(id = "assigned")
    private Select assignedRolesSelect;

    @FindBy(id = "realm-composite")
    private Select effectiveRolesSelect;

    @FindBy(id = "client-composite")
    private Select effectiveClientRolesSelect;

    @FindBy(id = "available-client")
    private Select availableClientRolesSelect;

    @FindBy(id = "assigned-client")
    private Select assignedClientRolesSelect;

    @FindBy(css = "button[ng-click*='addRealm']")
    private WebElement addSelectedRealmRolesButton;

    @FindBy(css = "button[ng-click*='addClient']")
    private WebElement addSelectedClientRolesButton;

    @FindBy(css = "button[ng-click*='deleteRealm']")
    private WebElement removeSelectedRealmRolesButton;

    @FindBy(css = "button[ng-click*='deleteClient']")
    private WebElement removeSelectedClientRolesButton;

    @FindBy(id = "clients")
    private Select clientRolesSelect;

    public void addAvailableRole(String... roles){
        waitGuiForElement(By.id("available"));
        for(String role : roles) {
            availableRolesSelect.selectByVisibleText(role);
            addSelectedRealmRolesButton.click();
        }
    }

    public void removeAssignedRole(String role){
        waitGuiForElement(By.id("assigned"));
        assignedRolesSelect.selectByVisibleText(role);
        removeSelectedRealmRolesButton.click();
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

    public boolean isAssignedClientRole(String role){
        waitGuiForElement(By.id("assigned"));
        try {
            assignedClientRolesSelect.selectByVisibleText(role);
        } catch (Exception ex){
            return false;
        }
        return true;
    }

    public void selectClientRole(String client){
        waitGuiForElement(By.id("clients"));
        clientRolesSelect.selectByVisibleText(client);
    }

    public void addAvailableClientRole(String... roles){
        waitGuiForElement(By.id("available-client"));
        for(String role : roles) {
            availableClientRolesSelect.selectByVisibleText(role);
            addSelectedClientRolesButton.click();
        }
    }

    public void removeAssignedClientRole(String client){
        waitGuiForElement(By.id("assigned-client"));
        assignedClientRolesSelect.selectByVisibleText(client);
        removeSelectedClientRolesButton.click();
    }

    public boolean checkIfEffectiveRealmRolesAreComplete(Role... roles){
        List<String> roleNames = new ArrayList<>();
        for (Role role : roles){
            roleNames.add(role.getName());
        }
        for (WebElement role : effectiveRolesSelect.getOptions()){
            roleNames.contains(role.getText());
            roleNames.remove(role.getText());
        }
        System.out.println(roles);
        System.out.println(roleNames);
        return roleNames.isEmpty();
    }



    public boolean checkIfEffectiveClientRolesAreComplete(Role... roles){
        List<String> roleNames = new ArrayList<>();
        for (Role role : roles){
            roleNames.add(role.getName());
        }
        for (WebElement role : effectiveRolesSelect.getOptions()){
            roleNames.contains(role.getText());
            roleNames.remove(role.getText());
        }
        System.out.println(roles);
        System.out.println(roleNames);
        return roleNames.isEmpty();
    }
}
