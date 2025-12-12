package org.keycloak.admin.api.client;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.RealmsAdminResource;

public class DefaultClientsApiGroup implements ClientsApiGroup {
    private final KeycloakSession session;
    private final RealmsAdminResource realmsAdminResource;

    public DefaultClientsApiGroup(KeycloakSession session) {
        assertApiEnabled();
        this.session = session;
        var auth = AdminRoot.authenticateRealmAdminRequest(session);

        // TODO: refine permissions
        if (!auth.getRealm().getName().equals(Config.getAdminRealm()) || !auth.hasRealmRole(AdminRoles.ADMIN)) {
            throw new NotAuthorizedException("Wrong permissions");
        }
        realmsAdminResource = new RealmsAdminResource(session, auth, new TokenManager());
    }

    @Path("realms/{realmName}/clients")
    @Override
    public ClientsApi clients(@PathParam("realmName") String realmName) {
        return new DefaultClientsApi(session, realmsAdminResource.getRealmAdmin(realmName));
    }

    public static void assertApiEnabled() {
        if (!isApiEnabled()) {
            throw new NotFoundException();
        }
    }

    public static boolean isApiEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_ADMIN_API_V2);
    }

}
