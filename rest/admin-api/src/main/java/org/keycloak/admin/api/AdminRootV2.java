package org.keycloak.admin.api;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.AdminCorsPreflightService;

@Provider
@Path("admin/api")
public class AdminRootV2 {

    @Context
    protected KeycloakSession session;

    @Path("")
    public AdminApi latestAdminApi() {
        // we could return the latest Admin API if no version is specified
        return session.getProvider(AdminApi.class);
    }

    @Path("v2")
    public AdminApi adminApi() {
        return session.getProvider(AdminApi.class);
    }

    @Path("{any:.*}")
    @OPTIONS
    @Operation(hidden = true)
    public Object preFlight() {
        return new AdminCorsPreflightService();
    }
}
