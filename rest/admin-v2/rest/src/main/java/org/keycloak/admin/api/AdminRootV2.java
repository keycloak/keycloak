package org.keycloak.admin.api;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.AdminCorsPreflightService;

import org.eclipse.microprofile.openapi.annotations.Operation;

@Provider
@Path("admin/api")
public class AdminRootV2 {

    @Context
    protected KeycloakSession session;

    @Path("v2")
    public AdminApi adminApi() {
        checkApiEnabled();
        return new DefaultAdminApi(session);
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
            throw new NotFoundException();
        }
    }

    public static boolean isAdminApiV2Enabled() {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_ADMIN_API_V2); // There's currently only Client API for the new Admin API v2
    }
}
