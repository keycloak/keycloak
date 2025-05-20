package org.keycloak.admin.api.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.keycloak.provider.Provider;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.resources.KeycloakOpenAPI;

import java.util.stream.Stream;

@Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public interface ClientsApi extends Provider {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all clients", description = "Returns a list of all clients in the realm")
    Stream<ClientRepresentation> getClients();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create or update a client", description = "Creates a new client or updates an existing client")
    ClientRepresentation createOrUpdateClient(ClientRepresentation client);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new client", description = "Creates a new client in the realm")
    ClientRepresentation createClient(ClientRepresentation client);

    @Path("{id}")
    @Operation(summary = "Get client by ID", description = "Returns a client resource for the specified client ID")
    ClientApi client(@PathParam("id") String id);
}
