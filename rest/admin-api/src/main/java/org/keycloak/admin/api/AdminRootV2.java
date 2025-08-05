package org.keycloak.admin.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.AdminCorsPreflightService;

@Provider
@Path("admin/api")
@RequestScoped
public class AdminRootV2 {

    @Context
    protected KeycloakSession session;

    @Inject
    AdminApi adminApi;

    @Path("")
    public AdminApi latestAdminApi() {
        // we could return the latest Admin API if no version is specified
        return adminApi;
    }

    @Path("v2")
    public AdminApi adminApi() {
        return adminApi;
    }

    @Path("{any:.*}")
    @OPTIONS
    @Operation(hidden = true)
    public Object preFlight() {
        return new AdminCorsPreflightService();
    }
}
