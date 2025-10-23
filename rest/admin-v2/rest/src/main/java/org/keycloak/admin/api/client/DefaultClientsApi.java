package org.keycloak.admin.api.client;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;
import org.keycloak.admin.api.FieldValidation;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.representations.admin.v2.validation.CreateClientDefault;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientService;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.keycloak.validation.jakarta.JakartaValidatorProvider;

public class DefaultClientsApi implements ClientsApi {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final HttpResponse response;
    private final ClientService clientService;
    private final JakartaValidatorProvider validator;

    public DefaultClientsApi(KeycloakSession session) {
        this.session = session;
        this.realm = Objects.requireNonNull(session.getContext().getRealm());
        this.clientService = session.getProvider(ClientService.class);
        this.response = session.getContext().getHttpResponse();
        this.validator = session.getProvider(JakartaValidatorProvider.class);
    }

    @Override
    public Stream<ClientRepresentation> getClients() {
        return clientService.getClients(realm, null, null, null);
    }

    @Override
    public ClientRepresentation createClient(@Valid ClientRepresentation client, FieldValidation fieldValidation) {
        try {
            validator.validate(client, CreateClientDefault.class);
            response.setStatus(Response.Status.CREATED.getStatusCode());
            return clientService.createOrUpdate(realm, client, false).representation();
        } catch (ServiceException e) {
            throw new WebApplicationException(e.getMessage(), e.getSuggestedResponseStatus().orElse(Response.Status.BAD_REQUEST));
        }
    }

    @Override
    public ClientApi client(@PathParam("id") String clientId) {
        var client = Optional.ofNullable(session.clients().getClientByClientId(realm, clientId)).orElseThrow(() -> new NotFoundException("Client cannot be found"));
        session.getContext().setClient(client);
        return session.getProvider(ClientApi.class);
    }

    @Override
    public void close() {

    }
}
