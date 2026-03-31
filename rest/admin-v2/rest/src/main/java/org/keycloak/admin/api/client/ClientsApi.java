package org.keycloak.admin.api.client;

import java.util.stream.Stream;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.resources.KeycloakOpenAPI;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS_V2)
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public interface ClientsApi {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all clients", description = "Returns a list of all clients in the realm")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = BaseClientRepresentation.class)))
    })
    Stream<BaseClientRepresentation> getClients();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new client", description = "Creates a new client in the realm")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = BaseClientRepresentation.class)))
    })
    Response createClient(@Valid BaseClientRepresentation client);

    @Path("{id}")
    ClientApi client(@PathParam("id") String id);
}
