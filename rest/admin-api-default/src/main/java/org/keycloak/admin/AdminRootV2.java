package org.keycloak.admin;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import org.keycloak.admin.api.AdminApi;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

@jakarta.ws.rs.ext.Provider
@Path("/v2")
public class AdminRootV2 {

    @Context
    protected KeycloakSession session;

    @Path("admin")
    public AdminApi adminApi() {
        return Optional.ofNullable(session.getProvider(AdminApi.class))
                .orElseThrow(() -> new BadRequestException("Cannot find provider for Admin API v2")); // TODO change to NotFound
    }
}
