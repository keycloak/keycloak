package org.keycloak.rest.admin.api.client;

import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.api.ListOptions;
import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.client.ClientService;
import org.keycloak.services.client.ClientService.ClientProjectionOptions;
import org.keycloak.services.client.DefaultClientService;
import org.keycloak.services.client.query.ClientQueryException;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public class DefaultClientsApi implements ClientsApi {

    private final KeycloakSession session;
    private final AdminPermissionEvaluator permissions;
    private final RealmModel realm;
    private final ClientService clientService;

    public DefaultClientsApi(@Nonnull KeycloakSession session,
                             @Nonnull RealmModel realm,
                             @Nonnull AdminPermissionEvaluator permissions) {
        this.session = session;
        this.realm = realm;
        this.permissions = permissions;
        this.clientService = new DefaultClientService(session, realm, permissions);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Stream<BaseClientRepresentation> getClients(ListOptions params) {
        try {
            var searchOptions = params.getQuery() != null ? new ClientService.ClientSearchOptions(params.getQuery()) : null;
            var sortAndSliceOptions = ClientService.normalizePagination(params.getOffset(), params.getLimit());
            return clientService.getClients(realm, new ClientProjectionOptions(params.getFields()), searchOptions,
                    sortAndSliceOptions);
        } catch (ClientQueryException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Override
    public Response createClient(@Valid BaseClientRepresentation client) {
        return Response.status(Response.Status.CREATED)
                .entity(clientService.createClient(realm, client))
                .build();
    }

    /**
     * When the path {@code clientId} does not resolve, return 403 if the caller
     * cannot list clients
     * (anti client-ID phishing), matching {@code ClientsResource#getClient} for
     * Admin API v1.
     */
    private void enforceAntiPhishingIfClientMissing(String clientId) {
        if (realm.getClientByClientId(clientId) == null && !permissions.clients().canList()) {
            throw new ForbiddenException();
        }
    }

    @Path("{id}")
    @Override
    public ClientApi client(@PathParam("id") String clientId) {
        enforceAntiPhishingIfClientMissing(clientId);
        return new DefaultClientApi(session, realm, clientId, permissions);
    }

}
