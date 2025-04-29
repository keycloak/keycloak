package org.keycloak.admin.api;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.keycloak.admin.api.AdminApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.AdminCorsPreflightService;

import java.util.Optional;

@jakarta.ws.rs.ext.Provider
@Path("v2")
public class AdminRootV2 {

    @Context
    protected KeycloakSession session;

    @Path("admin")
    public AdminApi adminApi() {
        return Optional.ofNullable(session.getProvider(AdminApi.class))
                .orElseThrow(() -> new NotFoundException("Cannot find provider for Admin API v2"));
    }

    @Path("{any:.*}")
    @OPTIONS
    @Operation(hidden = true)
    public Object preFlight() {
        return new AdminCorsPreflightService();
    }
}
