package org.keycloak.admin.api.client;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.groups.ConvertGroup;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.api.ChosenBySpi;
import org.keycloak.admin.api.FieldValidation;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.representations.admin.v2.validation.CreateClientDefault;
import org.keycloak.services.ServiceException;

import java.util.Optional;
import java.util.stream.Stream;

@RequestScoped
@ChosenBySpi
public class DefaultClientsApi implements ClientsApi {
    private RealmModel realm;
    private HttpResponse response;

    @Context
    KeycloakSession session;

    @Inject
    ClientApi clientApi;

    @PostConstruct
    public void init() {
        this.realm = session.getContext().getRealm();
        this.response = session.getContext().getHttpResponse();
    }

    @Override
    public Stream<ClientRepresentation> getClients() {
        return session.services().clients().getClients(realm, null, null, null);
    }

    @Override
    public ClientRepresentation createClient(@Valid @ConvertGroup(to = CreateClientDefault.class) ClientRepresentation client,
                                             @QueryParam("fieldValidation") FieldValidation fieldValidation) {
        try {
            response.setStatus(Response.Status.CREATED.getStatusCode());
            return session.services().clients().createOrUpdate(realm, client, false).representation();
        } catch (ServiceException e) {
            throw new WebApplicationException(e.getMessage(), e.getSuggestedResponseStatus().orElse(Response.Status.BAD_REQUEST));
        }
    }

    @Override
    public ClientApi client(@PathParam("id") String clientId) {
        var client = Optional.ofNullable(session.clients().getClientByClientId(realm, clientId))
                .orElseThrow(() -> new NotFoundException("Client cannot be found"));
        session.getContext().setClient(client);
        return clientApi;
    }

    @Override
    public void close() {

    }
}
