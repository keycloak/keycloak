package org.keycloak.admin.api;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminCorsPreflightService;

@Provider
@Path("admin/api")
public class AdminRootV2 {

    @Context
    protected KeycloakSession session;

    @Path("")
    public AdminApi latestAdminApi() {
        checkApiEnabled();
        // we could return the latest Admin API if no version is specified
        return session.getProvider(AdminApi.class);
    }

    @Path("v2")
    public AdminApi adminApi() {
        checkApiEnabled();
        return session.getProvider(AdminApi.class);
    }

    @Path("{any:.*}")
    @OPTIONS
    @Operation(hidden = true)
    public Object preFlight() {
        checkApiEnabled();
        return new AdminCorsPreflightService();
    }

    private void checkApiEnabled() {
        if (!isAdminApiV2Enabled()) {
            throw ErrorResponse.error("Admin API v2 not enabled", Response.Status.NOT_FOUND);
        }
    }

    public static boolean isAdminApiV2Enabled() {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_ADMIN_API_V2); // There's currently only Client API for the new Admin API v2
    }
}
