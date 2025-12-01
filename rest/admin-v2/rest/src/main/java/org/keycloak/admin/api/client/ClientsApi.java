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

import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.resources.KeycloakOpenAPI;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS_V2)
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public interface ClientsApi {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all clients", description = "Returns a list of all clients in the realm")
    Stream<ClientRepresentation> getClients();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new client", description = "Creates a new client in the realm")
    ClientRepresentation createClient(@Valid ClientRepresentation client);

    @Path("{id}")
    ClientApi client(@PathParam("id") String id);
}
