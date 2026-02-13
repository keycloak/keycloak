package org.keycloak.admin.api.client;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.validation.CreateClientDefault;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientService;
import org.keycloak.services.client.DefaultClientService;
import org.keycloak.services.resources.admin.ClientsResource;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.validation.jakarta.HibernateValidatorProvider;
import org.keycloak.validation.jakarta.JakartaValidatorProvider;

public class DefaultClientsApi implements ClientsApi {
    private final KeycloakSession session;
    private final AdminPermissionEvaluator permissions;
    private final RealmModel realm;
    private final ClientService clientService;
    private final JakartaValidatorProvider validator;

    // v1 resources
    private final RealmAdminResource realmAdminResource;
    private final ClientsResource clientsResource;

    public DefaultClientsApi(@Nonnull KeycloakSession session,
                             @Nonnull AdminPermissionEvaluator permissions,
                             @Nonnull RealmAdminResource realmAdminResource) {
        this.session = session;
        this.permissions = permissions;
        this.realmAdminResource = realmAdminResource;

        this.realm = Objects.requireNonNull(session.getContext().getRealm());
        this.clientService = new DefaultClientService(session, permissions, realmAdminResource);
        this.validator = new HibernateValidatorProvider();
        this.clientsResource = realmAdminResource.getClients();
    }

    @GET
    @Override
    public Stream<BaseClientRepresentation> getClients() {
        return clientService.getClients(realm);
    }

    @POST
    @Override
    public Response createClient(@Valid BaseClientRepresentation client) {
        try {
            DefaultClientApi.validateUnknownFields(client);
            validator.validate(client, CreateClientDefault.class);
            return Response.status(Response.Status.CREATED)
                    .entity(clientService.createOrUpdate(realm, client, false).representation())
                    .build();
        } catch (ServiceException e) {
            throw e.toWebApplicationException();
        }
    }

    @Path("{id}")
    @Override
    public ClientApi client(@PathParam("id") String clientId) {
        var client = Optional.ofNullable(session.clients().getClientByClientId(realm, clientId));
        return new DefaultClientApi(session, clientId, permissions, realmAdminResource, client.map(c -> clientsResource.getClient(c.getId())).orElse(null));
    }

}
