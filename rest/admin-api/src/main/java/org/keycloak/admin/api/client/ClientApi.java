package org.keycloak.admin.api.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.api.FieldValidation;
import org.keycloak.representations.admin.v2.ClientRepresentation;

import com.fasterxml.jackson.databind.JsonNode;

public interface ClientApi {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation getClient();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation createOrUpdateClient(ClientRepresentation client, @PathParam("fieldValidation") FieldValidation fieldValidation);

    @PUT
    @Consumes("application/merge-patch+json")
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation patchClient(JsonNode patch, @PathParam("fieldValidation") FieldValidation fieldValidation);

}
