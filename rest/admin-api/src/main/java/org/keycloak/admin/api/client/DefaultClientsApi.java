package org.keycloak.admin.api.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientService;

import java.util.stream.Stream;

public class DefaultClientsApi implements ClientsApi {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final HttpResponse response;
    private final ClientService clientService;

    public DefaultClientsApi(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
        this.clientService = session.services().clients();
        this.response = session.getContext().getHttpResponse();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Stream<ClientRepresentation> getClients(@QueryParam("runtime") @DefaultValue("false") boolean runtime) {
        ClientService.ClientSearchOptions searchOptions = ClientService.ClientSearchOptions.DEFAULT;
        ClientService.ClientProjectionOptions projectionOptions = ClientService.ClientProjectionOptions.DEFAULT;

        if (runtime) {
            projectionOptions = ClientService.ClientProjectionOptions.FULL_REPRESENTATION;
        }

        return clientService.getClients(realm, projectionOptions, searchOptions);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation createOrUpdateClient(ClientRepresentation client) {
        try {
            // TODO return 200, or 201 if did not exist
            response.setStatus(Response.Status.OK.getStatusCode());
            return clientService.createOrUpdateClient(realm, client);
        } catch (ServiceException e) {
            throw new WebApplicationException(e.getMessage(), e.getSuggestedResponseStatus().orElse(Response.Status.BAD_REQUEST));
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation createClient(ClientRepresentation client) {
        try {
            response.setStatus(Response.Status.CREATED.getStatusCode());
            return clientService.createClient(realm, client);
        } catch (ServiceException e) {
            throw new WebApplicationException(e.getMessage(), e.getSuggestedResponseStatus().orElse(Response.Status.BAD_REQUEST));
        }
    }

    @Path("{id}")
    @Override
    public ClientApi client(@PathParam("id") String clientId) {
        return new DefaultClientApi(session, clientId);
    }

    @Override
    public void close() {

    }
}
