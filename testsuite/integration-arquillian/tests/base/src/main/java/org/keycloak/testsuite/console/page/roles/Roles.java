package org.keycloak.testsuite.console.page.roles;

import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.fragment.Navigation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class Roles extends AdminConsoleRealm {

    @FindBy(xpath = "")
    private RoleTabs tabs;

    public RoleTabs tabs() {
        return tabs;
    }

    public class RoleTabs extends Navigation {

        @FindBy(linkText = "Realm Roles")
        private WebElement realmRolesTab;
        @FindBy(linkText = "Default Roles")
        private WebElement defaultRolesTab;

        public void realmRoles() {
            clickAndWaitForHeader(realmRolesTab);
        }

        public void defaultRoles() {
            clickAndWaitForHeader(defaultRolesTab);
        }

    }
    
    public RolesResource rolesResource() {
        return realmResource().roles();
    }

}
