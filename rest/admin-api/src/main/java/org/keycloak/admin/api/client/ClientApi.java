package org.keycloak.admin.api.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.api.FieldValidation;
import org.keycloak.representations.admin.v2.ClientRepresentation;

import com.fasterxml.jackson.databind.JsonNode;

public interface ClientApi {

    // TODO move these
    public static final String CONENT_TYPE_MERGE_PATCH = "application/merge-patch+json";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation getClient();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation createOrUpdateClient(ClientRepresentation client, @PathParam("fieldValidation") FieldValidation fieldValidation);

    @PATCH
    @Consumes({MediaType.APPLICATION_JSON_PATCH_JSON, CONENT_TYPE_MERGE_PATCH})
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation patchClient(JsonNode patch, @PathParam("fieldValidation") FieldValidation fieldValidation);

}
