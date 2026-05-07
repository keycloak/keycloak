package org.keycloak.scim.services;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.Scim;
import org.keycloak.scim.resource.common.Meta;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;
import org.keycloak.scim.resource.spi.SingletonResourceTypeProvider;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import static org.keycloak.scim.services.Error.badRequest;
import static org.keycloak.scim.services.Error.forbidden;
import static org.keycloak.scim.services.Error.invalidSyntax;
import static org.keycloak.scim.services.Error.resourceNotFound;
import static org.keycloak.scim.services.Error.toResponse;

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
            return invalidSyntax("Unexpected identifier");
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
    public Response get(@PathParam("id") String id,
                        @QueryParam("attributes") String attributes,
                        @QueryParam("excludedAttributes") String excludedAttributes) {
        List<String> attrList = attributes != null ? List.of(attributes.split(",")) : null;
        List<String> excludedList = excludedAttributes != null ? List.of(excludedAttributes.split(",")) : null;

        R resource = getResource(id, attrList, excludedList);

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
        try {
            Stream<R> stream = resourceTypeProvider.getAll(searchRequest)
                    .peek(this::setMetadata);

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
        } catch (Exception e) {
            return toResponse(session, e);
        }
    }

    @Path("{id}")
    @DELETE
    @Produces(APPLICATION_SCIM_JSON)
    public Response delete(@PathParam("id") String id) {
        try {
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
        } catch (Exception e) {
            return toResponse(session, e);
        }
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
            return invalidSyntax("Invalid reference to resource");
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
            return invalidSyntax("No PATCH op schema provided in request");
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
        } catch (UnrecognizedPropertyException upe) {
            String message = "Unrecognized attribute: " + upe.getPropertyName();
            throw new BadRequestException(invalidSyntax(message));
        } catch (Exception e) {
            throw new BadRequestException(badRequest("Unknown error parsing the request"));
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

    private Response onPersist(R resource, Status status, BiFunction<ScimResourceTypeProvider<R>, R, R> consumer) {
        try {
            R r = consumer.apply(resourceTypeProvider, resource);

            setMetadata(r);

            return Response.status(status).entity(r).build();
        } catch (Exception e) {
            return toResponse(session, e);
        }
    }

    private R getResource(String id) {
        return getResource(id, null, null);
    }

    private R getResource(String id, List<String> attributes, List<String> excludedAttributes) {
        if (id == null) {
            return null;
        }

        try {
            return resourceTypeProvider.get(id, attributes, excludedAttributes);
        } catch (ForbiddenException fe) {
            throw new jakarta.ws.rs.ForbiddenException(forbidden());
        }
    }
}
