package org.keycloak.testsuite.console.page.roles;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.openqa.selenium.By.tagName;

/**
 *
 * @author tkyjovsk
 */
public class RolesTable extends DataTable {

    public static final String ADD_ROLE = "Add Role";

    public static final String EDIT = "Edit";
    public static final String DELETE = "Delete";

    public List<RoleRepresentation> searchRoles(String searchPattern) {
        search(searchPattern);
        return getRolesFromTableRows();
    }

    public void addRole() {
        clickHeaderLink(ADD_ROLE);
    }

    public void clickRole(String name) {
        clickRowByLinkText(name);
    }

    public void editRole(String name) {
        clickRowActionButton(getRowByLinkText(name), EDIT);
    }

    public void deleteRole(String name) {
        clickRowActionButton(getRowByLinkText(name), DELETE);
    }

    public RoleRepresentation findRole(String name) {
        List<RoleRepresentation> roles = searchRoles(name);
        if (roles.isEmpty()) {
            return null;
        } else {
            assert 1 == roles.size();
            return roles.get(0);
        }
    }

    public boolean containsRole(String roleName) {
        for (RoleRepresentation r : getRolesFromTableRows()) {
            if (roleName.equals(r.getName())) {
                return true;
            }
        }
        return false;
    }

    public List<RoleRepresentation> getRolesFromTableRows() {
        List<RoleRepresentation> rows = new ArrayList<>();
        for (WebElement row : rows()) {
            RoleRepresentation role = getRoleFromRow(row);
            if (role != null) {
                rows.add(role);
            }
        }
        return rows;
    }

    public RoleRepresentation getRoleFromRow(WebElement row) {
        RoleRepresentation role = null;
        List<WebElement> tds = row.findElements(tagName("td"));
        if (!(tds.isEmpty() || getTextFromElement(tds.get(0)).isEmpty())) {
            role = new RoleRepresentation();
            role.setName(getTextFromElement(tds.get(0)));
            role.setComposite(Boolean.valueOf(getTextFromElement(tds.get(1))));
            role.setDescription(getTextFromElement(tds.get(2)));
        }
        return role;
    }

}
