package org.keycloak.admin.api.client;

import java.util.stream.Stream;

import org.keycloak.admin.api.FieldValidation;
import org.keycloak.provider.Provider;
import org.keycloak.representations.admin.v2.ClientRepresentation;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public interface ClientsApi extends Provider {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Stream<ClientRepresentation> getClients();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation createClient(ClientRepresentation client, @PathParam("fieldValidation") FieldValidation fieldValidation);

    @Path("{id}")
    ClientApi client(@PathParam("id") String id);
}

