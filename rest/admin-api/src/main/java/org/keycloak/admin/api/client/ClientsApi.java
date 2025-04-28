package org.keycloak.admin.api.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.stream.Stream;

public interface ClientsApi extends Provider {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Stream<ClientRepresentation> getClients();

    @Path("{name}")
    ClientApi client(@PathParam("name") String name);
}

