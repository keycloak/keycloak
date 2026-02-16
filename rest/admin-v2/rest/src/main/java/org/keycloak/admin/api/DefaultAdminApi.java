package org.keycloak.admin.api;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.keycloak.Config;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.admin.api.client.DefaultClientsApi;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.RealmsAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

public class DefaultAdminApi implements AdminApi {
    private final KeycloakSession session;
    private final AdminAuth auth;

    // v1 resources
    private final RealmAdminResource realmAdminResource;
    private final AdminPermissionEvaluator permissionEvaluator;

    public DefaultAdminApi(KeycloakSession session, String realmName) {
        this.session = session;
        this.auth = AdminRoot.authenticateRealmAdminRequest(session);

        // TODO: refine permissions
        if (!auth.getRealm().getName().equals(Config.getAdminRealm()) || !auth.hasRealmRole(AdminRoles.ADMIN)) {
            throw new NotAuthorizedException("Wrong permissions");
        }
        RealmsAdminResource realmsAdminResource = new RealmsAdminResource(session, auth, new TokenManager());
        this.realmAdminResource = realmsAdminResource.getRealmAdmin(realmName);
        
        // Create permission evaluator for the target realm
        RealmModel realm = session.getContext().getRealm();
        this.permissionEvaluator = AdminPermissions.evaluator(session, realm, auth);
    }

    @Path("clients/{version:v\\d+}")
    @Override
    public ClientsApi clients(@PathParam("version") String version) {
        return switch (version) {
            case "v2" -> new DefaultClientsApi(session, permissionEvaluator, realmAdminResource, auth);
            default -> throw new NotFoundException();
        };
    }
}
