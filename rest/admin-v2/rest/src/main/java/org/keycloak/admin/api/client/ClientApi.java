package org.keycloak.admin.api.client;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.PatchTypeNames;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

public interface ClientApi {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a client", description = "Returns a single client by its clientId")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BaseClientRepresentation.class))),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    BaseClientRepresentation getClient();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create or update a client", description = "Creates or updates a client in the realm")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BaseClientRepresentation.class))),
        @APIResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = BaseClientRepresentation.class)))
    })
    Response createOrUpdateClient(@Valid BaseClientRepresentation client);

    @PATCH
    @Consumes(PatchTypeNames.JSON_MERGE)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Patch a client", description = "Partially updates a client using JSON Merge Patch")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BaseClientRepresentation.class))),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    BaseClientRepresentation patchClient(JsonNode patch);

    @DELETE
    @Operation(summary = "Delete a client", description = "Deletes a client from the realm")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Client successfully deleted"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    Response deleteClient();
}
