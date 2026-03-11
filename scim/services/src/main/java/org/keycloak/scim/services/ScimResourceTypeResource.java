package org.keycloak.scim.services;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.filter.ScimFilterException;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.Scim;
import org.keycloak.scim.resource.common.Meta;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;
import org.keycloak.scim.resource.spi.SingletonResourceTypeProvider;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.theme.Theme;
import org.keycloak.util.JsonSerialization;

public class ScimResourceTypeResource<R extends ResourceTypeRepresentation> {

    private static final String APPLICATION_SCIM_JSON = "application/scim+json";

    private final KeycloakSession session;
    private final ScimResourceTypeProvider<R> resourceTypeProvider;
    private final Class<? extends ResourceTypeRepresentation> resourceTypeClazz;
    private final AdminEventBuilder adminEvent;

    public ScimResourceTypeResource(KeycloakSession session, ScimResourceTypeProvider<R> resourceTypeProvider, AdminEventBuilder adminEvent) {
        this.session = session;
        this.resourceTypeProvider = resourceTypeProvider;
        this.resourceTypeClazz = resourceTypeProvider.getResourceType();
        this.adminEvent = adminEvent.resource(resourceTypeProvider.getAdminEventResourceType());
    }

    @POST
    @Consumes({APPLICATION_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces(APPLICATION_SCIM_JSON)
    public Response create(InputStream is) {
        R resource = parseResourceTypePayload(is);

        if (resource.getId() != null) {
            return badRequest("Unexpected identifier");
        }

        return onPersist(resource, Status.CREATED,
                (rScimResourceTypeProvider, r) -> {
                    R created = resourceTypeProvider.create(r);
                    adminEvent.operation(OperationType.CREATE)
                            .resourcePath(session.getContext().getUri(), created.getId())
                            .representation(created)
                            .success();
                    return created;
                });
    }

    @Path("{id}")
    @GET
    @Produces(APPLICATION_SCIM_JSON)
    public Response get(@PathParam("id") String id) {
        R resource = getResource(id);

        if (resource == null) {
            return resourceNotFound(id);
        }

        setMetadata(resource);

        return Response.ok().entity(resource).build();
    }

    @GET
    @Produces(APPLICATION_SCIM_JSON)
    public Response getAll(@QueryParam("filter") String filterExpression,
                           @QueryParam("attributes") String attributes,
                           @QueryParam("excludedAttributes") String excludedAttributes,
                           @QueryParam("sortBy") String sortBy,
                           @QueryParam("sortOrder") String sortOrder,
                           @QueryParam("startIndex") Integer startIndex,
                           @QueryParam("count") Integer count) {
        // Delegate to common search logic
        return search(SearchRequest.builder().withFilter(filterExpression)
                        .withAttributes(attributes != null ? List.of(attributes.split(",")) : null)
                        .withExcludedAttributes(excludedAttributes != null ? List.of(excludedAttributes.split(",")) : null)
                        .withSortBy(sortBy)
                        .withSortOrder(sortOrder)
                        .withStartIndex(startIndex)
                        .withCount(count).build());
    }

    @Path(".search")
    @POST
    @Consumes({APPLICATION_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces(APPLICATION_SCIM_JSON)
    public Response search(SearchRequest searchRequest) {

        Stream<R> stream;
        try {
            stream = resourceTypeProvider.getAll(searchRequest)
                    .peek(this::setMetadata);
        } catch (ScimFilterException e) {
            return badRequest(e.getMessage(), "invalidFilter");
        }

        if (resourceTypeProvider instanceof SingletonResourceTypeProvider<R>) {
            return Response.ok().entity(stream
                            .findAny().orElseThrow(NotFoundException::new))
                    .build();
        }

        List<R> resources = stream.toList();
        Long totalResults = resourceTypeProvider.count(searchRequest);

        ListResponse<R> response = new ListResponse<>();
        response.setResources(resources);
        response.setTotalResults(totalResults.intValue());
        response.setStartIndex(searchRequest.getStartIndex() != null ? searchRequest.getStartIndex() : 1);
        response.setItemsPerPage(resources.size());

        return Response.ok().entity(response).build();
    }

    @Path("{id}")
    @DELETE
    @Produces(APPLICATION_SCIM_JSON)
    public Response delete(@PathParam("id") String id) {
        R resource = getResource(id);

        if (resource == null) {
            return resourceNotFound(id);
        }

        if (resourceTypeProvider.delete(id)) {
            adminEvent.operation(OperationType.DELETE)
                    .resourcePath(session.getContext().getUri())
                    .representation(resource)
                    .success();
            return Response.noContent().build();
        }

        return badRequest("Could not delete resource not found with id " + id);
    }

    @Path("{id}")
    @PUT
    @Consumes({APPLICATION_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces(APPLICATION_SCIM_JSON)
    public Response update(@PathParam("id") String id, InputStream is) {
        R existing = getResource(id);

        if (existing == null) {
            return resourceNotFound(id);
        }

        R resource = parseResourceTypePayload(is);

        if (!existing.getId().equals(resource.getId())) {
            return badRequest("Invalid reference to resource");
        }

        return onPersist(resource, Status.OK,
                (rScimResourceTypeProvider, r) -> {
                    R updated = resourceTypeProvider.update(r);
                    adminEvent.operation(OperationType.UPDATE)
                            .resourcePath(session.getContext().getUri())
                            .representation(updated)
                            .success();
                    return updated;
                });
    }

    @Path("{id}")
    @PATCH
    @Consumes({APPLICATION_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces(APPLICATION_SCIM_JSON)
    public Response patch(@PathParam("id") String id, PatchRequest request) {
        R existing = getResource(id);

        if (existing == null) {
            return resourceNotFound(id);
        }

        if (!request.getSchemas().contains(Scim.PATCH_OP_CORE_SCHEMA)) {
            return badRequest("Unsupported patch schema: " + Scim.PATCH_OP_CORE_SCHEMA, "invalidPatch");
        }

        return onPersist(existing, Status.OK, (rScimResourceTypeProvider, r) -> {
            resourceTypeProvider.patch(existing, request.getOperations());
            R patched = getResource(id);
            adminEvent.operation(OperationType.UPDATE)
                    .resourcePath(session.getContext().getUri())
                    .representation(patched)
                    .success();
            return patched;
        });
    }

    @SuppressWarnings("unchecked")
    private R parseResourceTypePayload(InputStream is) {
        try {
            return  (R) JsonSerialization.readValue(is, resourceTypeClazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize request body", e);
        }
    }

    private void setMetadata(R resource) {
        Meta meta = new Meta();
        meta.setResourceType(resourceTypeProvider.getName());
        Long createdTimestamp = resource.getCreatedTimestamp();
        Long lastModifiedTimestamp = resource.getLastModifiedTimestamp();
        if (createdTimestamp != null) {
            meta.setCreated(Instant.ofEpochMilli(createdTimestamp).toString());
        }
        if (lastModifiedTimestamp != null) {
            meta.setLastModified(Instant.ofEpochMilli(lastModifiedTimestamp).toString());
        }
        UriBuilder location = session.getContext().getUri().getAbsolutePathBuilder();
        if (resource.getId() != null) {
            location.path(resource.getId());
        }
        meta.setLocation(location.build().toString());
        resource.setMeta(meta);
    }

    private Properties getMessageBundle(String lang) {
        try {
            Theme theme = session.theme().getTheme(Theme.Type.ADMIN);
            Locale locale = lang != null ? Locale.forLanguageTag(lang) : Locale.ENGLISH;
            return theme.getMessages(locale);
        } catch (IOException e) {
            return new Properties();
        }
    }

    private Response onPersist(R resource, Status status, BiFunction<ScimResourceTypeProvider<R>, R, R> consumer) {
        try {
            R r = consumer.apply(resourceTypeProvider, resource);
            setMetadata(r);

            return Response.status(status).entity(r).build();
        } catch (ModelValidationException mve) {
            String language = session.getContext().getRequestHeaders().getHeaderString(HttpHeaders.ACCEPT_LANGUAGE);
            Properties messages = getMessageBundle(language);
            String format = messages.getProperty(mve.getMessage(), mve.getMessage())
                    .replace("{{", "{").replace("}}", "}")
                    .replace("'", "");
            String message = MessageFormat.format(format, mve.getParameters());
            session.getTransactionManager().setRollbackOnly();
            return badRequest(message);
        } catch (ForbiddenException fbe) {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    private R getResource(String id) {
        if (id == null) {
            return null;
        }

        try {
            return resourceTypeProvider.get(id);
        } catch (ForbiddenException fe) {
            throw new jakarta.ws.rs.ForbiddenException(fe);
        }
    }

    private Response resourceNotFound(String id) {
        return errorResponse(Status.NOT_FOUND, "Resource not found with id " + id);
    }

    private Response badRequest(String message) {
        return errorResponse(Status.BAD_REQUEST, message);
    }

    private Response badRequest(String message, String scimType) {
        ErrorResponse error = new ErrorResponse(message, Status.BAD_REQUEST.getStatusCode());
        error.setScimType(scimType);
        return Response.status(Status.BAD_REQUEST).entity(error).build();
    }

    private Response errorResponse(Status status, String message) {
        return Response.status(status).entity(new ErrorResponse(message, status.getStatusCode())).build();
    }
}
