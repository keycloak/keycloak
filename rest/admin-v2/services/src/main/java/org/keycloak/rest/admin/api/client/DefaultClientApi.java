package org.keycloak.rest.admin.api.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.PatchType;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.scim.ClientResourceTypeProvider;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientViewContext;
import org.keycloak.services.error.ProblemType;
import org.keycloak.services.util.ObjectMapperResolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;


public class DefaultClientApi implements ClientApi {
    
    private static final ObjectMapper MAPPER = new ObjectMapperResolver().getContext(null).copy()
            .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);
    
    private final KeycloakSession session;
    private final String clientId;
    private final ClientResourceTypeProvider typeProvider;

    public DefaultClientApi(@Nonnull KeycloakSession session,
                            @Nonnull String clientId,
                            @Nonnull ClientResourceTypeProvider typeProvider) {
        this.session = session;
        this.clientId = clientId;
        this.typeProvider = typeProvider;
    }

    private static ServiceException clientNotFound() {
        return new ServiceException(ProblemType.NOT_FOUND, "Cannot find the specified client");
    }

    @GET
    @Override
    public BaseClientRepresentation getClient() {
        try {
            BaseClientRepresentation result = Optional.ofNullable(typeProvider.get(clientId))
                    .orElseThrow(DefaultClientApi::clientNotFound);
            
            // matches v1 behavior - the event is only triggered on getting a single client,
            // not on a search, nor other logic that does typeProvider.get
            //
            // TODO: however this is not a great design - this logic is very specific to clients and surfaces more from
            // the type provider logic than it should
            session.clientPolicy().triggerOnEvent(new AdminClientViewContext(typeProvider.getModel(clientId), typeProvider.getPermissions().adminAuth()));
            return result;
        } catch (ClientPolicyException e) {
            throw new ServiceException(ProblemType.POLICY_VIOLATION, e.getErrorDetail());
        }
    }

    @PUT
    @Override
    public Response createOrUpdateClient(BaseClientRepresentation client) {
        if (client == null) {
            throw new ServiceException(ProblemType.MALFORMED_BODY, "A request body is required");
        }
        assertSameClientIds(clientId, client.getClientId());

        var current = typeProvider.get(clientId);

        if (current == null) {
            return Response.status(Response.Status.CREATED).entity(typeProvider.create(client)).build();
        }

        return Response.status(Response.Status.OK).entity(typeProvider.update(client)).build();
    }

    /**
     * v2 uses json merge patching rather than scim patching. For now the logic for this will be in the 
     * rest, rather than the type layer
     */
    @PATCH
    @Override
    public BaseClientRepresentation patchClient(InputStream patch) {
        String contentType = session.getContext().getHttpRequest().getHttpHeaders().getHeaderString(HttpHeaders.CONTENT_TYPE);
        PatchType patchType = PatchType.getByMediaType(contentType)
                .orElseThrow(() -> new ServiceException(ProblemType.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"));

        var current = typeProvider.get(clientId);

        if (current == null) {
            throw clientNotFound();
        }

        if (!typeProvider.hasPermission(current, AdminPermissionsSchema.MANAGE)) {
            throw new jakarta.ws.rs.ForbiddenException();
        }

        BaseClientRepresentation updated;
        switch (patchType) {
            case JSON_MERGE -> {
                JsonNode patchNode;
                try {
                    patchNode = MAPPER.readValue(patch, JsonNode.class);
                    if (!patchNode.isObject()) {
                        throw new ServiceException(ProblemType.INVALID_PATCH, "Cannot replace client resource with non-object");
                    }
                    final ObjectReader objectReader = MAPPER.readerForUpdating(current);
                    updated = objectReader.readValue(patchNode);
                } catch (JsonMappingException e) {
                    if (e.getPath().isEmpty()) {
                        throw new ServiceException(ProblemType.INVALID_PATCH, "Invalid patch");
                    }
                    var invalidField = e.getPath().stream().map(JsonMappingException.Reference::getFieldName).collect(Collectors.joining("."));
                    throw new ServiceException(ProblemType.INVALID_PATCH, "Invalid value for %s".formatted(invalidField));
                } catch (JsonProcessingException e) {
                    throw new ServiceException(ProblemType.MALFORMED_BODY, "Cannot parse the JSON");
                } catch (IOException e) {
                    throw new ServiceException(ProblemType.INTERNAL_ERROR, "Unable to read the request body");
                }
            }
            default -> throw new ServiceException(ProblemType.UNSUPPORTED_MEDIA_TYPE, "Invalid patch type");
        }

        assertSameClientIds(clientId, updated.getClientId());

        return typeProvider.update(updated);
    }
    
    protected void assertSameClientIds(String pathId, String payloadId) {
        if (payloadId == null) {
            // When the payload clientId is null, it is not part of the payload at all - validated via @NotBlank validator annotation
            return;
        }
        if (!Objects.equals(pathId, payloadId)) {
            throw ServiceException.invalid("clientId", "Field 'clientId' in payload does not match the provided 'clientId'");
        }
    }

    @DELETE
    @Override
    public Response deleteClient() {
        if (typeProvider.get(clientId) == null) {
            throw clientNotFound();
        }
        typeProvider.delete(clientId); // TODO: not currently using the boolean return
        return Response.noContent().build();
    }
}
