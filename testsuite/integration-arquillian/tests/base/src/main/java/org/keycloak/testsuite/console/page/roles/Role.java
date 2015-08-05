package org.keycloak.testsuite.console.page.roles;

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

    private RoleForm form;

    public RoleForm form() {
        return form;
    }

}
