package org.keycloak.admin.api.client;

import java.io.IOException;
import java.util.Objects;

import org.keycloak.http.HttpResponse;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientService;
import org.keycloak.services.client.DefaultClientService;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.resources.admin.ClientsResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.fabric8.zjsonpatch.JsonPatch;
import io.fabric8.zjsonpatch.JsonPatchException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class DefaultClientApi implements ClientApi {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final ClientModel client;
    private final ClientService clientService;
    private HttpResponse response;

    private final ClientResource clientResource;
    private final ClientsResource clientsResource;
    private final String clientId;

    public DefaultClientApi(KeycloakSession session, ClientsResource clientsResource, ClientResource clientResource, String clientId) {
        this.session = session;
        this.realm = Objects.requireNonNull(session.getContext().getRealm());
        this.client = Objects.requireNonNull(session.getContext().getClient());
        this.clientService = new DefaultClientService(session);
        this.response = session.getContext().getHttpResponse();
        this.clientsResource = clientsResource;
        this.clientResource = clientResource;
        this.clientId = clientId;
    }

    @Override
    public ClientRepresentation getClient() {
        return clientService.getClient(clientResource, realm, client.getClientId(), null)
                .orElseThrow(() -> new NotFoundException("Cannot find the specified client"));
    }

    @Override
    public ClientRepresentation createOrUpdateClient(ClientRepresentation client) {
        try {
            if (!Objects.equals(clientId, client.getClientId())) {
                throw new WebApplicationException("cliendId in payload does not match the clientId in the path", Response.Status.BAD_REQUEST);
            }
            validateUnknownFields(client, response);
            var result = clientService.createOrUpdate(clientsResource, clientResource, realm, client, true);
            if (result.created()) {
                response.setStatus(Response.Status.CREATED.getStatusCode());
            }
            return result.representation();
        } catch (ServiceException e) {
            throw new WebApplicationException(e.getMessage(), e.getSuggestedResponseStatus().orElse(Response.Status.BAD_REQUEST));
        }
    }

    @Override
    public ClientRepresentation patchClient(JsonNode patch) {
        // patches don't yet allow for creating
        ClientRepresentation client = getClient();
        try {
            String contentType = session.getContext().getHttpRequest().getHttpHeaders().getHeaderString(HttpHeaders.CONTENT_TYPE);

            ClientRepresentation updated = null;

            // TODO: there should be a more centralized objectmapper
            ObjectMapper objectMapper = new ObjectMapper();
            if (MediaType.valueOf(contentType).getSubtype().equals(MediaType.APPLICATION_JSON_PATCH_JSON_TYPE.getSubtype())) {
                JsonNode patchedNode = JsonPatch.apply(patch, objectMapper.convertValue(client, JsonNode.class));
                updated = objectMapper.convertValue(patchedNode, ClientRepresentation.class);
            } else { // must be merge patch
                final ObjectReader objectReader = objectMapper.readerForUpdating(client);
                updated = objectReader.readValue(patch);
            }

            validateUnknownFields(updated, response);
            return clientService.createOrUpdate(clientsResource, clientResource, realm, updated, true).representation();
        } catch (JsonPatchException e) {
            // TODO: kubernetes uses 422 instead
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorResponse.error("Unknown Error Occurred", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    static void validateUnknownFields(ClientRepresentation rep, HttpResponse response) {
        if (!rep.getAdditionalFields().isEmpty()) {
            throw new WebApplicationException("Payload contains unknown fields: " + rep.getAdditionalFields().keySet(), Response.Status.BAD_REQUEST);
        }
    }

}
