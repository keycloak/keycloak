package org.keycloak.admin.api.client;

import java.util.Objects;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.PatchType;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientService;
import org.keycloak.services.client.DefaultClientService;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import com.fasterxml.jackson.databind.JsonNode;


public class DefaultClientApi implements ClientApi {
    private final KeycloakSession session;
    private final String clientId;
    private final RealmModel realm;
    private final ClientService clientService;

    public DefaultClientApi(@Nonnull KeycloakSession session,
                            @Nonnull String clientId,
                            @Nonnull AdminPermissionEvaluator permissions,
                            @Nonnull RealmAdminResource realmAdminResource,
                            @Nullable ClientResource clientResource) {
        this.session = session;
        this.clientId = clientId;
        this.clientService = new DefaultClientService(session, permissions, realmAdminResource, clientResource);
        this.realm = Objects.requireNonNull(session.getContext().getRealm());
    }

    @GET
    @Override
    public BaseClientRepresentation getClient() {
        try {
            return clientService.getClient(realm, clientId)
                    .orElseThrow(() -> new NotFoundException("Cannot find the specified client"));
        } catch (ServiceException e) {
            throw e.toWebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @PUT
    @Override
    public Response createOrUpdateClient(BaseClientRepresentation client) {
        if (!Objects.equals(clientId, client.getClientId())) {
            throw new WebApplicationException("clientId in payload does not match the clientId in the path", Response.Status.BAD_REQUEST);
        }
        var result = clientService.createOrUpdate(realm, client, true);
        return Response.status(result.created() ? Response.Status.CREATED : Response.Status.OK).entity(result.representation()).build();
    }

    @PATCH
    @Override
    public BaseClientRepresentation patchClient(JsonNode patch) {
        String contentType = session.getContext().getHttpRequest().getHttpHeaders().getHeaderString(HttpHeaders.CONTENT_TYPE);
        PatchType patchType = PatchType.getByMediaType(contentType)
                .orElseThrow(() -> new WebApplicationException("Unsupported media type", Response.Status.UNSUPPORTED_MEDIA_TYPE));

        return clientService.patchClient(realm, clientId, patchType, patch);
    }

    @DELETE
    @Override
    public void deleteClient() {
        clientService.deleteClient(realm, clientId);
    }
}
