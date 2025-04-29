package org.keycloak.admin.api.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.api.mapper.ApiModelMapper;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;

import java.util.stream.Stream;

public class DefaultClientsApi implements ClientsApi {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final ApiModelMapper mapper;
    private final HttpResponse response;

    public DefaultClientsApi(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.response = session.getContext().getHttpResponse();
        this.mapper = session.getProvider(ApiModelMapper.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Stream<ClientRepresentation> getClients() {
        return realm.getClientsStream().map(mapper.clients()::fromModel);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation createOrUpdateClient(ClientRepresentation client) {
        // TODO return 200, or 201 if did not exist
        response.setStatus(Response.Status.OK.getStatusCode());
        return null;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation createClient(ClientRepresentation client) {
        if (realm.getClientByClientId(client.getClientId()) != null) {
            throw new WebApplicationException("Client already exists", Response.Status.CONFLICT.getStatusCode());
        }

        var model = realm.addClient(client.getClientId());
        response.setStatus(Response.Status.CREATED.getStatusCode());
        return mapper.clients().fromModel(model);
    }

    @Path("{name}")
    @Override
    public ClientApi client(@PathParam("name") String name) {
        return new DefaultClientApi(session, name);
    }

    @Override
    public void close() {

    }
}
