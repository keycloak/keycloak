package org.keycloak.admin.api.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.models.mapper.ModelMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;

import java.util.Optional;

public class DefaultClientApi implements ClientApi {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final String clientId;
    private final ModelMapper mapper;

    public DefaultClientApi(KeycloakSession session, String clientId) {
        this.session = session;
        this.clientId = clientId;
        this.realm = session.getContext().getRealm();
        this.mapper = session.getProvider(ModelMapper.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public ClientRepresentation getClient(@QueryParam("runtime") Boolean isRuntime) {
        if (isRuntime != null && isRuntime) {
            // return whole representation
            var client = Optional.ofNullable(session.clients().getClientByClientId(realm, clientId))
                    .orElseThrow(() -> new NotFoundException("Cannot find the specified client"));
            return mapper.clients().fromModel(client);
        }
        return getTestReducedClientRep(clientId); // TODO return only what was posted/putted
    }

    private static ClientRepresentation getTestReducedClientRep(String clientId) {
        return new ClientRepresentation(clientId);
    }
}
