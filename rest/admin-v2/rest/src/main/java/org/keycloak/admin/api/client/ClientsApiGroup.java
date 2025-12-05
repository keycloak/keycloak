package org.keycloak.admin.api.client;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

public interface ClientsApiGroup {

    @Path("realms/{realmName}/clients")
    ClientsApi clients(@PathParam("realmName") String realmName);
}
