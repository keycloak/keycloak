package org.keycloak.admin.api.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.provider.Provider;
import org.keycloak.representations.admin.v2.ClientRepresentation;

import java.util.stream.Stream;

public interface ClientsApi extends Provider {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Stream<ClientRepresentation> getClients();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation createOrUpdateClient(ClientRepresentation client);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation createClient(ClientRepresentation client);

    @Path("{id}")
    ClientApi client(@PathParam("id") String id);
}

