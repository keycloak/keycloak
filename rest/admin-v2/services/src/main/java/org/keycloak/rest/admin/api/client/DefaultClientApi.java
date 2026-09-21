package org.keycloak.rest.admin.api.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.services.PatchType;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.scim.ClientResourceTypeProvider;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientViewContext;
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

    @GET
    @Override
    public BaseClientRepresentation getClient() {
        try {
            BaseClientRepresentation result = Optional.ofNullable(typeProvider.get(clientId))
                    .orElseThrow(() -> new NotFoundException("Cannot find the specified client"));
            
            // matches v1 behavior - the event is only triggered on getting a single client,
            // not on a search, nor other logic that does typeProvider.get
            //
            // TODO: however this is not a great design - this logic is very specific to clients and surfaces more from
            // the type provider logic than it should
            session.clientPolicy().triggerOnEvent(new AdminClientViewContext(typeProvider.getModel(clientId), typeProvider.getPermissions().adminAuth()));
            return result;
        } catch (ClientPolicyException e) {
            throw new WebApplicationException(e.getErrorDetail(), Response.Status.BAD_REQUEST);
        } catch (ServiceException e) {
            throw e.toWebApplicationException();
        } catch (ForbiddenException e) {
            throw new jakarta.ws.rs.ForbiddenException();
        } 
    }

    @PUT
    @Override
    public Response createOrUpdateClient(BaseClientRepresentation client) {
        assertSameClientIds(clientId, client.getClientId());

        try {
            var current = typeProvider.get(clientId);
            
            if (current == null) {
                return Response.status(Response.Status.CREATED).entity(typeProvider.create(client)).build();
            }
            
            return Response.status(Response.Status.OK).entity(typeProvider.update(client)).build();
        } catch (ServiceException e) {
            throw e.toWebApplicationException();
        } catch (ForbiddenException e) {
            throw new jakarta.ws.rs.ForbiddenException();
        }
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
                .orElseThrow(() -> new WebApplicationException("Unsupported media type", Response.Status.UNSUPPORTED_MEDIA_TYPE));
        
        try {
            var current = typeProvider.get(clientId);
            
            if (!typeProvider.hasPermission(current, AdminPermissionsSchema.MANAGE)) {
                throw new jakarta.ws.rs.ForbiddenException();
            }
            
            if (current == null) {
                throw new NotFoundException("Cannot find the specified client");
            }
            
            BaseClientRepresentation updated;
            switch (patchType) {
                case JSON_MERGE -> {
                    JsonNode patchNode;
                    try {
                        patchNode = MAPPER.readValue(patch, JsonNode.class);
                        if (!patchNode.isObject()) {
                            throw new WebApplicationException("Cannot replace client resource with non-object", Response.Status.BAD_REQUEST);
                        }
                        final ObjectReader objectReader = MAPPER.readerForUpdating(current);
                        updated = objectReader.readValue(patchNode);
                    } catch (JsonMappingException e) {
                        if (e.getPath().isEmpty()) {
                            throw new WebApplicationException("Invalid patch", Response.Status.BAD_REQUEST);
                        }
                        var invalidField = e.getPath().stream().map(JsonMappingException.Reference::getFieldName).collect(Collectors.joining("."));
                        throw new WebApplicationException("Invalid value for %s".formatted(invalidField), Response.Status.BAD_REQUEST);
                    } catch (JsonProcessingException e) {
                        throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
                    } catch (IOException e) {
                        throw new WebApplicationException("Unknown Error Occurred", Response.Status.INTERNAL_SERVER_ERROR);
                    }
                }
                default -> throw new WebApplicationException("Invalid patch type", Response.Status.UNSUPPORTED_MEDIA_TYPE);
            }
            
            assertSameClientIds(clientId, updated.getClientId());

            return typeProvider.update(updated);
        } catch (ServiceException e) {
            throw e.toWebApplicationException();
        } catch (ForbiddenException e) {
            throw new jakarta.ws.rs.ForbiddenException();
        }
    }
    
    protected void assertSameClientIds(String pathId, String payloadId) {
        if (payloadId == null) {
            // When the payload clientId is null, it is not part of the payload at all - validated via @NotBlank validator annotation
            return;
        }
        if (!Objects.equals(pathId, payloadId)) {
            throw new WebApplicationException("Field 'clientId' in payload does not match the provided 'clientId'", Response.Status.BAD_REQUEST);
        }
    }

    @DELETE
    @Override
    public Response deleteClient() {
        try {
            typeProvider.delete(clientId); // TODO: not currently using the boolean return
        } catch (ServiceException e) {
            throw e.toWebApplicationException();
        } catch (ForbiddenException e) {
            // TODO: a common error handler should be considered 
            throw new jakarta.ws.rs.ForbiddenException();
        }
        return Response.noContent().build();
    }
}
