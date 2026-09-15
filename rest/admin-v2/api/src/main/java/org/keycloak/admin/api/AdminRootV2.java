package org.keycloak.admin.api;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("admin/api")
public interface AdminRootV2 {

    @Path("{realmName}")
    AdminApi adminApi(@PathParam("realmName") String realmName);

    // TODO Fix preflights
    @Path("{realmName}/{any:.*}")
    @OPTIONS
    @Operation(hidden = true)
    Response preFlight();
}
