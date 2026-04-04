package org.keycloak.admin.api.client;

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
import org.keycloak.services.client.ClientService;
import org.keycloak.services.client.ClientServiceHelper;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public class DefaultClientsApi implements ClientsApi {
    private final KeycloakSession session;
    private final AdminPermissionEvaluator permissions;
    private final RealmModel realm;
    private final ClientService clientService;

    // v1 resources
    private final RealmAdminResource realmAdminResource;

    public DefaultClientsApi(@Nonnull KeycloakSession session,
                             @Nonnull RealmModel realm,
                             @Nonnull AdminPermissionEvaluator permissions,
                             // remove v1 resource once we are not attached to API v1
                             @Nonnull RealmAdminResource realmAdminResource) {
        this.session = session;
        this.realm = realm;
        this.permissions = permissions;
        this.realmAdminResource = realmAdminResource;
        this.clientService = ClientServiceHelper.getClientService(session, realm, permissions, realmAdminResource);
    }

    @GET
    @Override
    public Stream<BaseClientRepresentation> getClients() {
        return clientService.getClients(realm);
    }

    @POST
    @Override
    public Response createClient(@Valid BaseClientRepresentation client) {
        return Response.status(Response.Status.CREATED)
                .entity(clientService.createClient(realm, client))
                .build();
    }

    @Path("{id}")
    @Override
    public ClientApi client(@PathParam("id") String clientId) {
        return new DefaultClientApi(session, realm, clientId, permissions, realmAdminResource);
    }

}
