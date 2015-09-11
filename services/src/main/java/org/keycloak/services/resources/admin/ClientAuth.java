package org.keycloak.services.resources.admin;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientRoles;
import org.keycloak.models.RoleModel;
import org.keycloak.services.ForbiddenException;

public class ClientAuth {

    private RealmAuth realmAuth;
    private ClientModel clientApp;

    public ClientAuth(RealmAuth realmAuth, ClientModel clientApp) {
        this.realmAuth = realmAuth;
        this.clientApp = clientApp;
    }

    public AdminAuth getAuth() {
        return getRealmAuth().getAuth();
    }

    public RealmAuth getRealmAuth() {
        return realmAuth;
    }

    public boolean isClientAdmin() {
        return getAuth().hasAppRole(clientApp, ClientRoles.CLIENT_ADMIN);
    }

    public boolean isRealmAdmin() {
        return isGlobalAdmin() || getAuth().hasRealmRole(AdminRoles.REALM_ADMIN);
    }

    public boolean isGlobalAdmin() {
        return getAuth().hasRealmRole(AdminRoles.ADMIN);
    }

    public boolean isAdmin() {
        return isClientAdmin() || isRealmAdmin();
    }

    public boolean hasAppRole(ClientModel app, String role) {
        RoleModel roleModel = app.getRole(role);
        return roleModel != null && getAuth().getUser().hasRole(roleModel);
    }

    public boolean hasOneOfAppRole(String... roles) {
        for (String role : roles) {
            if (hasAppRole(clientApp, role)) {
                return true;
            }
        }
        return false;
    }

    public boolean canView() {
        return getRealmAuth().hasView() && (!clientApp.isClientManageAuthEnabled() ||
                hasOneOfAppRole(ClientRoles.MANAGE_CLIENT, ClientRoles.VIEW_CLIENT) || isRealmAdmin());
    }

    public boolean canManage() {
        return getRealmAuth().hasManage() && (!clientApp.isClientManageAuthEnabled() ||
                hasOneOfAppRole(ClientRoles.MANAGE_CLIENT) || isRealmAdmin());
    }

    public boolean canDelegate() {
        return getRealmAuth().hasManage() && (!clientApp.isClientManageAuthEnabled() ||
                hasOneOfAppRole(ClientRoles.MANAGE_USER_ROLES) || isRealmAdmin());
    }

    public void requireView() {
        if (!canView()) {
            throw new ForbiddenException();
        }
    }

    public void requireManage() {
        if (!canManage()) {
            throw new ForbiddenException();
        }
    }

    public void requireDelegate() {
        if (!canDelegate()) {
            throw new ForbiddenException();
        }
    }
}
