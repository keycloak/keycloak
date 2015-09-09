package org.keycloak.services.resources.admin;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientRoles;
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

    public boolean isAdmin() {
        return getAuth().hasAppRole(clientApp, ClientRoles.CLIENT_ADMIN);
    }

    public boolean canView() {
        return getRealmAuth().hasView() && (!clientApp.isClientManageAuthEnabled() ||
                getAuth().hasOneOfAppRole(clientApp, ClientRoles.MANAGE_CLIENT, ClientRoles.VIEW_CLIENT));
    }

    public boolean canManage() {
        return getRealmAuth().hasManage() && (!clientApp.isClientManageAuthEnabled() ||
                getAuth().hasOneOfAppRole(clientApp, ClientRoles.MANAGE_CLIENT));
    }

    public boolean canDelegate() {
        return getRealmAuth().hasManage() && (!clientApp.isClientManageAuthEnabled() ||
                getAuth().hasOneOfAppRole(clientApp, ClientRoles.MANAGE_USER_ROLES));
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
