package org.keycloak.admin.api.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public interface ClientApi {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation getClient();
}
