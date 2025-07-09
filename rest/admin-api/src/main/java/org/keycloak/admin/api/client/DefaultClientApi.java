package org.keycloak.admin.api.client;

import java.io.IOException;

import org.keycloak.admin.api.FieldValidation;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientService;

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
    private final String clientId;
    private final ClientService clientService;
    private HttpResponse response;

    public DefaultClientApi(KeycloakSession session, String clientId) {
        this.session = session;
        this.clientId = clientId;
        this.realm = session.getContext().getRealm();
        this.clientService = session.services().clients();
        this.response = session.getContext().getHttpResponse();
    }

    @Override
    public ClientRepresentation getClient() {
        return clientService.getClient(realm, clientId, null)
                .orElseThrow(() -> new NotFoundException("Cannot find the specified client"));
    }

    @Override
    public ClientRepresentation createOrUpdateClient(ClientRepresentation client, FieldValidation fieldValidation) {
        try {
            var result = clientService.createOrUpdate(realm, client, true);
            if (result.created()) {
                response.setStatus(Response.Status.CREATED.getStatusCode());
            }
            return result.representation();
        } catch (ServiceException e) {
            throw new WebApplicationException(e.getMessage(), e.getSuggestedResponseStatus().orElse(Response.Status.BAD_REQUEST));
        }
    }

    @Override
    public ClientRepresentation patchClient(JsonNode patch, FieldValidation fieldValidation) {
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

            // TODO: reuse in the other methods
            if (!updated.getAdditionalFields().isEmpty()) {
                if (fieldValidation == null || fieldValidation == FieldValidation.Strict) {
                    // validation failed
                    throw new WebApplicationException("Payload contains unknown fields: " + updated.getAdditionalFields().keySet(), Response.Status.BAD_REQUEST);
                } else if (fieldValidation == FieldValidation.Warn) {
                    response.addHeader("WARNING", "Payload contains unknown fields: " + updated.getAdditionalFields().keySet());
                }
            }
            return clientService.createOrUpdate(realm, updated, true).representation();
        } catch (JsonPatchException e) {
            // TODO: kubernetes uses 422 instead
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorResponse.error("Unknown Error Occurred", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
