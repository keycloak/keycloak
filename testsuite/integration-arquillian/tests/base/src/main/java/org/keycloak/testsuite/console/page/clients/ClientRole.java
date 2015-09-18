package org.keycloak.testsuite.console.page.clients;

import org.keycloak.testsuite.console.page.roles.*;

/**
 *
 * @author tkyjovsk
 */
public class ClientRole extends ClientRoles {

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

    private RoleForm form;

    public RoleForm form() {
        return form;
    }

    public void backToClientRolesViaBreadcrumb() {
        breadcrumb().clickItemOneLevelUp();
    }

}
