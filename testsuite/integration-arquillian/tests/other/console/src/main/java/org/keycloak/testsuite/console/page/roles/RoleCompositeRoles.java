package org.keycloak.testsuite.console.page.roles;

import org.keycloak.representations.idm.RoleRepresentation.Composites;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

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
            clientRoles.put(client, new ArrayList<>(getSelectValues(assignedClientRolesSelect)));
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
        clickLink(button);
    }

    public void addRealmRole(String role) {
        addMissingRoles(availableRealmRolesSelect, addSelectedRealmRolesButton, Arrays.asList(role));
    }
    
    public void addClientRole(String role) {
        addMissingRoles(availableClientRolesSelect, addSelectedClientRolesButton, Arrays.asList(role));
    }
    
    protected void addMissingRoles(Select select, WebElement button, Collection<String> roles) {
        select.deselectAll();
        if (roles != null) { // if roles not provided, don't add any
            for (String role : getSelectValues(select)) {
                if (roles.contains(role)) { // if roles provided, add only the missing
                    select.selectByVisibleText(role);
                }
            }
            waitUntilElement(button).is().enabled();
            clickLink(button);
        }
    }

    public static Set<String> getSelectValues(Select select) {
        Set<String> roles = new HashSet<>();
        for (WebElement option : select.getOptions()) {
            roles.add(getTextFromElement(option));
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
        waitUntilElement(By.id("available")).is().present();
        for (String role : roles) {
            availableRealmRolesSelect.selectByVisibleText(role);
            clickLink(addSelectedRealmRolesButton);
        }
    }

    public void removeAssignedRole(String role) {
        waitUntilElement(By.id("assigned")).is().present();
        assignedRealmRolesSelect.selectByVisibleText(role);
        clickLink(removeSelectedRealmRolesButton);
    }

    public boolean isAssignedRole(String role) {
        waitUntilElement(By.id("assigned")).is().present();
        return UIUtils.selectContainsOption(assignedRealmRolesSelect, role);
    }

    public boolean isAssignedClientRole(String role) {
        waitUntilElement(By.id("assigned")).is().present();
        return UIUtils.selectContainsOption(assignedClientRolesSelect, role);
    }

    public void selectClientRole(String client) {
        waitUntilElement(By.id("clients")).is().present();
        clientSelect.selectByVisibleText(client);
    }

    public void addAvailableClientRole(String... roles) {
        waitUntilElement(By.id("available-client")).is().present();
        for (String role : roles) {
            availableClientRolesSelect.selectByVisibleText(role);
            clickLink(addSelectedClientRolesButton);
        }
    }

    public void removeAssignedClientRole(String client) {
        waitUntilElement(By.id("assigned-client")).is().present();
        assignedClientRolesSelect.selectByVisibleText(client);
        clickLink(removeSelectedClientRolesButton);
    }

}
