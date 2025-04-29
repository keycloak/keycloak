package org.keycloak.admin.api.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.client.ClientService;

public class DefaultClientApi implements ClientApi {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final String clientId;
    private final ClientService clientService;

    public DefaultClientApi(KeycloakSession session, String clientId) {
        this.session = session;
        this.clientId = clientId;
        this.realm = session.getContext().getRealm();
        this.clientService = session.services().clients();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public ClientRepresentation getClient(@QueryParam("runtime") Boolean isRuntime) {
        return clientService.getClient(realm, clientId, isRuntime)
                .orElseThrow(() -> new NotFoundException("Cannot find the specified client"));
    }
}
