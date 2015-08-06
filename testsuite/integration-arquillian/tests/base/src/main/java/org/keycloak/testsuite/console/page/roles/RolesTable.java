package org.keycloak.testsuite.console.page.roles;

import java.util.ArrayList;
import java.util.List;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import static org.keycloak.testsuite.util.SeleniumUtils.waitAjaxForElement;
import static org.openqa.selenium.By.linkText;
import static org.openqa.selenium.By.tagName;
import org.openqa.selenium.WebElement;

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
        waitAjaxForElement(body());
        body().findElement(linkText(name)).click();
    }

    public void clickRole(RoleRepresentation role) {
        clickRole(role.getName());
    }

    public void editRole(String name) {
        clickActionButton(getRowByLinkText(name), EDIT);
    }

    public void editRole(RoleRepresentation role) {
        editRole(role.getName());
    }

    public void deleteRole(RoleRepresentation role) {
        deleteRole(role.getName());
    }

    public void deleteRole(String name) {
        deleteRole(name, true);
    }

    public void deleteRole(String name, boolean confirm) {
        clickActionButton(getRowByLinkText(name), DELETE);
        if (confirm) {
            modalDialog.confirmDeletion();
        } else {
            modalDialog.cancel();
        }
    }

    public RoleRepresentation findRole(String name) {
        List<RoleRepresentation> roles = searchRoles(name);
        assert 1 == roles.size();
        return roles.get(0);
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
        if (!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
            role = new RoleRepresentation();
            role.setName(tds.get(0).getText());
            role.setComposite(Boolean.valueOf(tds.get(1).getText()));
            role.setDescription(tds.get(2).getText());
        }
        return role;
    }

}
