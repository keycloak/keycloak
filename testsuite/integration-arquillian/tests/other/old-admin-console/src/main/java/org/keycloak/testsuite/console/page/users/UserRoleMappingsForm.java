package org.keycloak.testsuite.console.page.users;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.console.page.roles.RoleCompositeRoles;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * Created by fkiss.
 */
public class UserRoleMappingsForm extends RoleCompositeRoles {

    @FindBy(id = "realm-composite")
    private Select effectiveRolesSelect;

    @FindBy(id = "client-composite")
    private Select effectiveClientRolesSelect;

    public boolean isEffectiveRealmRolesComplete(RoleRepresentation... roles) {
        return isEffectiveRolesComplete(effectiveRolesSelect, roles);
    }

    public boolean isEffectiveClientRolesComplete(RoleRepresentation... roles) {
        return isEffectiveRolesComplete(effectiveClientRolesSelect, roles);
    }

    private boolean isEffectiveRolesComplete(Select select, RoleRepresentation... roles) {
        List<String> roleNames = new ArrayList<>();
        for (RoleRepresentation role : roles) {
            roleNames.add(role.getName());
        }
        for (WebElement role : select.getOptions()) {
            roleNames.contains(getTextFromElement(role));
            roleNames.remove(getTextFromElement(role));
        }
        log.info(Arrays.toString(roles));
        log.info(roleNames);
        return roleNames.isEmpty();
    }

}
