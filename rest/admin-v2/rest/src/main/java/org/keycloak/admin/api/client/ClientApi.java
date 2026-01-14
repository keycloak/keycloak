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

import com.fasterxml.jackson.databind.JsonNode;

import static org.keycloak.admin.api.AdminApi.CONTENT_TYPE_MERGE_PATCH;

public interface ClientApi {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    BaseClientRepresentation getClient();

    /**
     * @return {@link BaseClientRepresentation} of created/updated client
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createOrUpdateClient(@Valid BaseClientRepresentation client);

    @PATCH
    @Consumes(CONTENT_TYPE_MERGE_PATCH)
    @Produces(MediaType.APPLICATION_JSON)
    BaseClientRepresentation patchClient(JsonNode patch);

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    void deleteClient();

}
