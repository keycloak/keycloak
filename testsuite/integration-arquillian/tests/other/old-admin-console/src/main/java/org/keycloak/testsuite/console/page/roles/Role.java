package org.keycloak.testsuite.console.page.roles;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 *
 * @author tkyjovsk
 */
public class Role extends RealmRoles {

    public static final String ROLE_ID = "roleId";

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/{" + ROLE_ID + "}";
    }

    public void setRoleId(String id) {
        setUriParameter(ROLE_ID, id);
    }

    public String getRoleId() {
        return getUriParameter(ROLE_ID).toString();
    }

    @FindBy(css = "ul.nav-tabs")
    private RoleTabs tabs;

    public RoleTabs roleTabs() {
        return tabs;
    }

    public class RoleTabs {
        @FindBy(linkText = "Details")
        private WebElement detailsTab;

        @FindBy(linkText = "Permissions")
        private WebElement permissionsTab;

        @FindBy(linkText = "Users in Role")
        private WebElement usersInRoleTab;

        public void details() {
            clickLink(detailsTab);
        }

        public void permissions() {
            clickLink(permissionsTab);
        }

        public void usersInRole() {
            clickLink(usersInRoleTab);
        }
    }

}
