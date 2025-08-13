package org.keycloak.admin.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.fabric8.zjsonpatch.JsonPatch;
import io.fabric8.zjsonpatch.JsonPatchException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.api.ChosenBySpi;
import org.keycloak.admin.api.FieldValidation;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.mapper.ModelMapper;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientService;

import java.io.IOException;

@RequestScoped
@ChosenBySpi
public class DefaultClientApi implements ClientApi {
    private RealmModel realm;
    private ClientModel client;
    private ClientService clientService;
    private ClientModelMapper mapper;
    private HttpResponse response;

    @Context
    KeycloakSession session;

    @PostConstruct
    public void init() {
        this.realm = session.getContext().getRealm();
        this.client = session.getContext().getClient();
        this.clientService = session.services().clients();
        this.mapper = session.getProvider(ModelMapper.class).clients();
        this.response = session.getContext().getHttpResponse();
    }

    @Override
    public ClientRepresentation getClient() {
        if (client == null) {
            throw new NotFoundException("Cannot find the specified client");
        }
        return mapper.fromModel(client);
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

    @Override
    public void close() {

    }
}
