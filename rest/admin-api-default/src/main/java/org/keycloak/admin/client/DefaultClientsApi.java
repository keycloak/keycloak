package org.keycloak.admin.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.admin.api.client.ClientRepresentation;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.admin.api.mapper.ApiModelMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.stream.Stream;

public class DefaultClientsApi implements ClientsApi {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final ApiModelMapper mapper;

    public DefaultClientsApi(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.mapper = session.getProvider(ApiModelMapper.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Stream<ClientRepresentation> getClients() {
        return realm.getClientsStream().map(mapper::fromModel);
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
