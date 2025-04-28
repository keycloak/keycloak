package org.keycloak.admin.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.admin.api.client.ClientRepresentation;
import org.keycloak.admin.api.mapper.ApiModelMapper;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class DefaultClientApi implements ClientApi {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final ClientModel client;
    private final ApiModelMapper mapper;

    public DefaultClientApi(KeycloakSession session, String name) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.client = session.clients()
                .getClientsStream(realm) // TODO we should have better approach to retrieve the client
                .filter(f -> f.getName().equals(name))
                .findAny()
                .orElseThrow(() -> new NotFoundException("Cannot find the specified client"));
        session.getContext().setClient(this.client);
        this.mapper = session.getProvider(ApiModelMapper.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public ClientRepresentation getClient() {
        return mapper.fromModel(client);
    }
}
