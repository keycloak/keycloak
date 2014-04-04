package org.keycloak.services.resources.admin;

import org.keycloak.models.AdminRoles;
import org.keycloak.services.managers.Auth;

import javax.ws.rs.ForbiddenException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmAuth {

    private Resource resource;

    public enum Resource {
        APPLICATION, CLIENT, USER, REALM, AUDIT
    }

    private Auth auth;
    private String realmAdminApp;

    public RealmAuth(Auth auth, String realmAdminApp) {
        this.auth = auth;
        this.realmAdminApp = realmAdminApp;
    }

    public RealmAuth init(Resource resource) {
        this.resource = resource;
        return this;
    }

    public void requireAny() {
        if (!auth.hasOneOfAppRole(realmAdminApp, AdminRoles.ALL_REALM_ROLES)) {
            throw new ForbiddenException();
        }
    }

    public boolean hasView() {
        return auth.hasOneOfAppRole(realmAdminApp, getViewRole(resource), getManageRole(resource));
    }

    public boolean hasManage() {
        return auth.hasOneOfAppRole(realmAdminApp, getManageRole(resource));
    }

    public void requireView() {
        if (!hasView()) {
            throw new ForbiddenException();
        }
    }

    public void requireManage() {
        if (!hasManage()) {
            throw new ForbiddenException();
        }
    }

    private String getViewRole(Resource resource) {
        switch (resource) {
            case APPLICATION:
                return AdminRoles.VIEW_APPLICATIONS;
            case CLIENT:
                return AdminRoles.VIEW_CLIENTS;
            case USER:
                return AdminRoles.VIEW_USERS;
            case REALM:
                return AdminRoles.VIEW_REALM;
            case AUDIT:
                return AdminRoles.VIEW_AUDIT;
            default:
                throw new IllegalStateException();
        }
    }

    private String getManageRole(Resource resource) {
        switch (resource) {
            case APPLICATION:
                return AdminRoles.MANAGE_APPLICATIONS;
            case CLIENT:
                return AdminRoles.MANAGE_CLIENTS;
            case USER:
                return AdminRoles.MANAGE_USERS;
            case REALM:
                return AdminRoles.MANAGE_REALM;
            case AUDIT:
                return AdminRoles.MANAGE_AUDIT;
            default:
                throw new IllegalStateException();
        }
    }

}
