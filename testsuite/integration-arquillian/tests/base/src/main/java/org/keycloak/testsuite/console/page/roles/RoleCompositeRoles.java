package org.keycloak.testsuite.console.page.roles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.keycloak.representations.idm.RoleRepresentation.Composites;
import org.keycloak.testsuite.page.Form;
import static org.keycloak.testsuite.util.WaitUtils.waitGuiForElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author fkiss
 * @author tkyjovsk
 */
public class RoleCompositeRoles extends Form {

    @FindBy(id = "available")
    protected Select availableRealmRolesSelect;
    @FindBy(id = "assigned")
    protected Select assignedRealmRolesSelect;

    @FindBy(id = "clients")
    protected Select clientSelect;
    @FindBy(id = "available-client")
    protected Select availableClientRolesSelect;
    @FindBy(id = "assigned-client")
    protected Select assignedClientRolesSelect;

    @FindBy(css = "button[ng-click*='addRealm']")
    protected WebElement addSelectedRealmRolesButton;
    @FindBy(css = "button[ng-click*='addClient']")
    protected WebElement addSelectedClientRolesButton;
    @FindBy(css = "button[ng-click*='deleteRealm']")
    protected WebElement removeSelectedRealmRolesButton;
    @FindBy(css = "button[ng-click*='deleteClient']")
    protected WebElement removeSelectedClientRolesButton;

    public Composites getComposites() {
        Composites composites = new Composites();
        // realm roles
        composites.setRealm(getSelectValues(assignedRealmRolesSelect));
        // client roles
        Map<String, List<String>> clientRoles = new HashMap<>();
        for (String client : getSelectValues(clientSelect)) {
            clientSelect.selectByVisibleText(client);
            clientRoles.put(client, new ArrayList(getSelectValues(assignedClientRolesSelect)));
        }
        composites.setClient(clientRoles);
        return composites;
    }

    public void setComposites(Composites composites) {
        if (composites != null) {
            setRealmRoles(composites.getRealm());
            for (String client : composites.getClient().keySet()) {
                clientSelect.selectByVisibleText(client);
                setClientRoles(composites.getClient().get(client));
            }
        }
    }

    private void setRealmRoles(Collection<String> roles) {
        removeRedundantRoles(assignedRealmRolesSelect, removeSelectedRealmRolesButton, roles);
        addMissingRoles(availableRealmRolesSelect, addSelectedRealmRolesButton, roles);
    }

    private void setClientRoles(Collection<String> roles) {
        removeRedundantRoles(assignedClientRolesSelect, removeSelectedClientRolesButton, roles);
        addMissingRoles(availableClientRolesSelect, addSelectedClientRolesButton, roles);
    }

    private void removeRedundantRoles(Select select, WebElement button, Collection<String> roles) {
        select.deselectAll();
        for (String role : getSelectValues(select)) {
            if (roles == null // if roles not provided, remove all
                    || !roles.contains(role)) { // if roles provided, remove only the redundant
                select.selectByVisibleText(role);
            }
        }
        button.click();
    }

    protected void addMissingRoles(Select select, WebElement button, Collection<String> roles) {
        select.deselectAll();
        if (roles != null) { // if roles not provided, don't add any
            for (String role : getSelectValues(select)) {
                if (roles.contains(role)) { // if roles provided, add only the missing
                    select.selectByVisibleText(role);
                }
            }
            button.click();
        }
    }

    public static Set<String> getSelectValues(Select select) {
        Set<String> roles = new HashSet<>();
        for (WebElement option : select.getOptions()) {
            roles.add(option.getText());
        }
        return roles;
    }

    // ***
    public Set<String> getAvailableRealmRoles() {
        return getSelectValues(availableRealmRolesSelect);
    }

    public Set<String> getAvailableClientRoles(String client) {
        return getSelectValues(availableClientRolesSelect);
    }

    public Set<String> getAssignedRealmRoles() {
        return getSelectValues(assignedRealmRolesSelect);
    }

    public Set<String> getAssignedClientRoles() {
        return getSelectValues(assignedClientRolesSelect);
    }

    // *** original methods ***
    public void addAvailableRole(String... roles) {
        waitGuiForElement(By.id("available"));
        for (String role : roles) {
            availableRealmRolesSelect.selectByVisibleText(role);
            addSelectedRealmRolesButton.click();
        }
    }

    public void removeAssignedRole(String role) {
        waitGuiForElement(By.id("assigned"));
        assignedRealmRolesSelect.selectByVisibleText(role);
        removeSelectedRealmRolesButton.click();
    }

    public boolean isAssignedRole(String role) {
        waitGuiForElement(By.id("assigned"));
        try {
            assignedRealmRolesSelect.selectByVisibleText(role);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public boolean isAssignedClientRole(String role) {
        waitGuiForElement(By.id("assigned"));
        try {
            assignedClientRolesSelect.selectByVisibleText(role);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public void selectClientRole(String client) {
        waitGuiForElement(By.id("clients"));
        clientSelect.selectByVisibleText(client);
    }

    public void addAvailableClientRole(String... roles) {
        waitGuiForElement(By.id("available-client"));
        for (String role : roles) {
            availableClientRolesSelect.selectByVisibleText(role);
            addSelectedClientRolesButton.click();
        }
    }

    public void removeAssignedClientRole(String client) {
        waitGuiForElement(By.id("assigned-client"));
        assignedClientRolesSelect.selectByVisibleText(client);
        removeSelectedClientRolesButton.click();
    }

}
