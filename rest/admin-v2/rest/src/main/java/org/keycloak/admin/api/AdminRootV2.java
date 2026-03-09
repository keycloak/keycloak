package org.keycloak.admin.api;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;


@Path("admin/api")
public interface AdminRootV2 {

    @Path("{realmName}")
    AdminApi adminApi(@PathParam("realmName") String realmName);

    @Path("{realmName}/{any:.*}")
    @OPTIONS
    Response preFlight();
}
