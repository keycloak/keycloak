package org.keycloak.admin.api.client;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.validation.Valid;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.representations.admin.v2.validation.CreateClientDefault;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientService;
import org.keycloak.services.client.DefaultClientService;
import org.keycloak.services.resources.admin.ClientsResource;
import org.keycloak.validation.jakarta.HibernateValidatorProvider;
import org.keycloak.validation.jakarta.JakartaValidatorProvider;

public class DefaultClientsApi implements ClientsApi {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final HttpResponse response;
    private final ClientService clientService;
    private final JakartaValidatorProvider validator;
    private final ClientsResource clientsResource;

    public DefaultClientsApi(KeycloakSession session, ClientsResource clientsResource) {
        this.session = session;
        this.realm = Objects.requireNonNull(session.getContext().getRealm());
        this.clientService = new DefaultClientService(session);
        this.response = session.getContext().getHttpResponse();
        this.validator = new HibernateValidatorProvider();
        this.clientsResource = clientsResource;
    }

    @Override
    public Stream<ClientRepresentation> getClients() {
        return clientService.getClients(clientsResource, realm, null, null, null);
    }

    @Override
    public ClientRepresentation createClient(@Valid ClientRepresentation client) {
        try {
            DefaultClientApi.validateUnknownFields(client, response);
            validator.validate(client, CreateClientDefault.class);
            response.setStatus(Response.Status.CREATED.getStatusCode());
            return clientService.createOrUpdate(clientsResource, null, realm, client, false).representation();
        } catch (ServiceException e) {
            throw new WebApplicationException(e.getMessage(), e.getSuggestedResponseStatus().orElse(Response.Status.BAD_REQUEST));
        }
    }

    @Override
    public ClientApi client(@PathParam("id") String clientId) {
        var client = Optional.ofNullable(session.clients().getClientByClientId(realm, clientId));
        return new DefaultClientApi(session, clientsResource, client.map(c -> clientsResource.getClient(c.getId())).orElse(null), clientId);
    }

}
