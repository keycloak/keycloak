package org.keycloak.admin.api.client;

import java.util.stream.Stream;

import org.keycloak.admin.api.FieldValidation;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientService;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

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

    @Override
    @GET
    public Stream<ClientRepresentation> getClients() {
        return clientService.getClients(realm, null, null, null);
    }

    @Override
    public ClientRepresentation createClient(ClientRepresentation client, FieldValidation fieldValidation) {
        try {
            response.setStatus(Response.Status.CREATED.getStatusCode());
            return clientService.createOrUpdate(realm, client, false).representation();
        } catch (ServiceException e) {
            throw new WebApplicationException(e.getMessage(), e.getSuggestedResponseStatus().orElse(Response.Status.BAD_REQUEST));
        }
    }

    @Override
    public ClientApi client(@PathParam("id") String clientId) {
        return new DefaultClientApi(session, clientId);
    }

    @Override
    public void close() {

    }
}
