package org.keycloak.admin.api.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.representations.admin.v2.ClientRepresentation;

public interface ClientApi {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation getClient(@QueryParam("runtime") Boolean isRuntime);
}
