package org.keycloak.admin.api;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.admin.api.client.DefaultClientsApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.RealmsAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.validation.jakarta.HibernateValidatorProvider;

public class DefaultAdminApi implements AdminApi {
    private final ApiContext context;

    // v1 resources
    private final RealmAdminResource realmResource;

    public DefaultAdminApi(KeycloakSession session, String realmName) {
        var authInfo = AdminRoot.authenticateRealmAdminRequest(session);
        var permissions = AdminPermissions.evaluator(session, new RealmManager(session).getRealmByName(realmName), authInfo);
        var tokenManager = new TokenManager();
        var authContext = new ApiContext.AuthContext(authInfo, permissions, tokenManager);
        this.context = new ApiContext(session, new HibernateValidatorProvider(), authContext);

        this.realmResource = new RealmsAdminResource(session, authInfo, tokenManager).getRealmAdmin(realmName);
    }

    @Path("clients/{version:v\\d+}")
    @Override
    public ClientsApi clients(@PathParam("version") String version) {
        return switch (version) {
            case "v2" -> new DefaultClientsApi(context, realmResource);
            default -> throw new NotFoundException();
        };
    }
}
