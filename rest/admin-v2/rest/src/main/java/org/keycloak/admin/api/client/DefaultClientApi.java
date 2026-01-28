package org.keycloak.admin.api.client;

import java.io.IOException;
import java.util.Objects;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.api.AdminApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientService;
import org.keycloak.services.client.DefaultClientService;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.util.ObjectMapperResolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class DefaultClientApi implements ClientApi {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final ClientService clientService;

    private final ClientResource clientResource;
    private final String clientId;
    private final ObjectMapper objectMapper;

    private static final ObjectMapper MAPPER = new ObjectMapperResolver().getContext(null);

    public DefaultClientApi(KeycloakSession session, RealmAdminResource realmAdminResource, ClientResource clientResource, String clientId) {
        this.session = session;
        this.clientResource = clientResource;
        this.clientId = clientId;

        this.realm = Objects.requireNonNull(session.getContext().getRealm());
        this.clientService = new DefaultClientService(session, realmAdminResource, clientResource);

        this.objectMapper = MAPPER;
    }

    @GET
    @Override
    public BaseClientRepresentation getClient() {
        return clientService.getClient(realm, clientId, null)
                .orElseThrow(() -> new NotFoundException("Cannot find the specified client"));
    }

    @PUT
    @Override
    public Response createOrUpdateClient(BaseClientRepresentation client) {
        try {
            if (!Objects.equals(clientId, client.getClientId())) {
                throw new WebApplicationException("cliendId in payload does not match the clientId in the path", Response.Status.BAD_REQUEST);
            }
            validateUnknownFields(client);
            var result = clientService.createOrUpdate(realm, client, true);
            return Response.status(result.created() ? Response.Status.CREATED : Response.Status.OK).entity(result.representation()).build();
        } catch (ServiceException e) {
            throw new WebApplicationException(e.getMessage(), e.getSuggestedResponseStatus().orElse(Response.Status.BAD_REQUEST));
        }
    }

    @PATCH
    @Override
    public BaseClientRepresentation patchClient(JsonNode patch) {
        BaseClientRepresentation client = getClient();
        try {
            String contentType = session.getContext().getHttpRequest().getHttpHeaders().getHeaderString(HttpHeaders.CONTENT_TYPE);
            MediaType mediaType = contentType == null ? null : MediaType.valueOf(contentType);
            MediaType mergePatch = MediaType.valueOf(AdminApi.CONTENT_TYPE_MERGE_PATCH);
            if (mediaType == null || !mediaType.isCompatible(mergePatch)) {
                throw new WebApplicationException("Unsupported media type", Response.Status.UNSUPPORTED_MEDIA_TYPE);
            }

            final ObjectReader objectReader = objectMapper.readerForUpdating(client);
            BaseClientRepresentation updated = objectReader.readValue(patch);

            validateUnknownFields(updated);
            return clientService.createOrUpdate(realm, updated, true).representation();
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Unsupported media type", Response.Status.UNSUPPORTED_MEDIA_TYPE);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorResponse.error("Unknown Error Occurred", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Override
    public void deleteClient() {
        if (clientResource == null) {
            throw new NotFoundException("Cannot find the specified client");
        }
        clientResource.deleteClient();
    }

    static void validateUnknownFields(BaseClientRepresentation rep) {
        if (rep.getAdditionalFields().keySet().stream().anyMatch(k -> !k.equals(BaseClientRepresentation.DISCRIMINATOR_FIELD))) {
            throw new WebApplicationException("Payload contains unknown fields: " + rep.getAdditionalFields().keySet(), Response.Status.BAD_REQUEST);
        }
    }

}
