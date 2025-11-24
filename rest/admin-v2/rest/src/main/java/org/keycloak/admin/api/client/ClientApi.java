package org.keycloak.admin.api.client;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.representations.admin.v2.ClientRepresentation;

import com.fasterxml.jackson.databind.JsonNode;

public interface ClientApi {

    // TODO move these
    String CONTENT_TYPE_MERGE_PATCH = "application/merge-patch+json";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation getClient();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation createOrUpdateClient(@Valid ClientRepresentation client);

    @PATCH
    @Consumes(CONTENT_TYPE_MERGE_PATCH)
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation patchClient(JsonNode patch);

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    void deleteClient();

}
