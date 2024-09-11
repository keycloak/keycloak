package org.keycloak.services.resources.service;

import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.resource.ClientService;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientViewContext;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.util.Optional;

public class DefaultClientService implements ClientService {
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;

    public DefaultClientService(KeycloakSession session) {
        this.session = session;
        this.auth = null; // have auth as part of context?
    }

    // obtained from the org.keycloak.services.resources.admin.ClientResource#getClient
    // TODO not having the REST exceptions
    // TODO obtain Auth permissions
    @Override
    public Optional<ClientRepresentation> getClient(RealmModel realm, String clientId) {
        var client = session.clients().getClientByClientId(realm, clientId);

        if (client == null) return Optional.empty();

        try {
            session.clientPolicy().triggerOnEvent(new AdminClientViewContext(client, auth.adminAuth()));
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        auth.clients().requireView(client);

        return Optional.ofNullable(session.modelMapper().fromModel(client));
    }

    // TODO have other methods
}
