package org.keycloak.admin.api;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.admin.api.client.DefaultClientsApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.RealmsAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

public class DefaultAdminApi implements AdminApi {
    private final KeycloakSession session;
    private final AdminPermissionEvaluator permissions;

    // v1 resources
    private final RealmAdminResource realmAdminResource;

    public DefaultAdminApi(KeycloakSession session, String realmName) {
        this.session = session;
        var authInfo = AdminRoot.authenticateRealmAdminRequest(session);
        this.permissions = AdminPermissions.evaluator(session, authInfo.getRealm(), authInfo);
        this.realmAdminResource = new RealmsAdminResource(session, authInfo, new TokenManager()).getRealmAdmin(realmName);
    }

    @Path("clients/{version:v\\d+}")
    @Override
    public ClientsApi clients(@PathParam("version") String version) {
        return switch (version) {
            case "v2" -> new DefaultClientsApi(session, permissions, realmAdminResource);
            default -> throw new NotFoundException();
        };
    }
}
