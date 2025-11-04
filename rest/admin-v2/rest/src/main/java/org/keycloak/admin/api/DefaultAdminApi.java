package org.keycloak.admin.api;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Path;
import org.keycloak.Config;
import org.keycloak.admin.api.realm.DefaultRealmsApi;
import org.keycloak.admin.api.realm.RealmsApi;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminRoot;

public class DefaultAdminApi implements AdminApi {
    private final KeycloakSession session;
    private final AdminAuth auth;

    public DefaultAdminApi(KeycloakSession session) {
        this.session = session;
        this.auth = AdminRoot.authenticateRealmAdminRequest(session);

        // TODO: refine permissions
        if (!auth.getRealm().getName().equals(Config.getAdminRealm()) || !auth.hasRealmRole(AdminRoles.ADMIN)) {
            throw new NotAuthorizedException("Wrong permissions");
        }
    }

    @Path("realms")
    @Override
    public RealmsApi realms() {
        return new DefaultRealmsApi(session);
    }

    @Override
    public void close() {

    }
}
