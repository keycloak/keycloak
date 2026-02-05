package org.keycloak.scim.services;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.scim.resource.ScimResource;
import org.keycloak.scim.resource.common.Meta;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;
import org.keycloak.util.JsonSerialization;

public class ScimResourceTypeResource<R extends ScimResource> {

    private static final String APPLICATION_SCIM_JSON = "application/scim+json";

    private final KeycloakSession session;
    private final ScimResourceTypeProvider<R> resourceTypeProvider;
    private final Class<? extends ScimResource> resourceTypeClazz;

    public ScimResourceTypeResource(KeycloakSession session, ScimResourceTypeProvider<R> resourceTypeProvider) {
        this.session = session;
        this.resourceTypeProvider = resourceTypeProvider;
        this.resourceTypeClazz = getResourceTypeClass(resourceTypeProvider);
    }

    @POST
    @Consumes(APPLICATION_SCIM_JSON)
    @Produces(APPLICATION_SCIM_JSON)
    public Response create(InputStream is) {
        R resource = parseResourceTypePayload(is);

        if (resource.getId() != null) {
            return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Unexpected identifier", Status.BAD_REQUEST.getStatusCode())).build();
        }

        try {
            resourceTypeProvider.validate(resource);
        } catch (ModelValidationException mve) {
            return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(mve.getMessage(), Status.BAD_REQUEST.getStatusCode())).build();
        }

        resource = resourceTypeProvider.create(resource);

        setMetadata(resource, Time.currentTimeMillis());

        return Response.status(201).entity(resource).build();
    }

    @Path("{id}")
    @GET
    @Produces(APPLICATION_SCIM_JSON)
    public Response get(@PathParam("id") String id) {
        R resource = resourceTypeProvider.get(id);

        if (resource == null) {
            return Response.status(Status.NOT_FOUND).entity(new ErrorResponse("Resource not found with id " + id, Status.NOT_FOUND.getStatusCode())).build();
        }

        setMetadata(resource, resource.getCreatedTimestamp());

        return Response.ok().entity(resource).build();
    }

    @Path("{id}")
    @DELETE
    @Produces(APPLICATION_SCIM_JSON)
    public Response delete(@PathParam("id") String id) {
        R resource = resourceTypeProvider.get(id);

        if (resource == null) {
            return Response.status(Status.NOT_FOUND).entity(new ErrorResponse("Resource not found with id " + id, Status.NOT_FOUND.getStatusCode())).build();
        }

        if (resourceTypeProvider.delete(id)) {
            return Response.noContent().build();
        }

        return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Could not delete resource not found with id " + id, Status.BAD_REQUEST.getStatusCode())).build();
    }

    @Path("{id}")
    @PUT
    @Consumes(APPLICATION_SCIM_JSON)
    @Produces(APPLICATION_SCIM_JSON)
    public Response update(@PathParam("id") String id, InputStream is) {
        R existing = resourceTypeProvider.get(id);

        if (existing == null) {
            return Response.status(Status.NOT_FOUND).entity(new ErrorResponse("Resource not found with id " + id, Status.NOT_FOUND.getStatusCode())).build();
        }

        R resource = parseResourceTypePayload(is);

        try {
            resourceTypeProvider.validate(resource);
        } catch (ModelValidationException mve) {
            return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(mve.getMessage(), Status.BAD_REQUEST.getStatusCode())).build();
        }

        resourceTypeProvider.update(resource);

        return Response.ok().entity(resource).build();
    }

    private R parseResourceTypePayload(InputStream is) {
        try {
            return  (R) JsonSerialization.readValue(is, resourceTypeClazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize request body", e);
        }
    }

    private Class<? extends ScimResource> getResourceTypeClass(ScimResourceTypeProvider<R> resourceTypeProvider) {
        Type[] genericInterfaces = resourceTypeProvider.getClass().getGenericInterfaces();

        if (genericInterfaces.length == 0) {
            Type type = resourceTypeProvider.getClass().getGenericSuperclass();

            if (type != null) {
                genericInterfaces = new Type[] { type };
            }
        }

        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericInterface;
                if (ScimResourceTypeProvider.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                    Type[] actualTypeArguments = pt.getActualTypeArguments();
                    if (actualTypeArguments.length > 1) {
                        if (actualTypeArguments[1] instanceof Class) {
                            return (Class<? extends ScimResource>) actualTypeArguments[1];
                        }
                    }
                }
            }
        }

        throw new RuntimeException("Could not determine resource type class from resource type provider " + resourceTypeProvider.getClass());
    }

    private void setMetadata(R resource, long createdTimestamp) {
        Meta meta = new Meta();
        meta.setResourceType(resource.getType().name());
        meta.setCreated(Instant.ofEpochMilli(createdTimestamp).toString());
        meta.setLastModified(meta.getCreated());
        meta.setLocation(session.getContext().getUri().getAbsolutePathBuilder().path(resource.getId()).build().toString());
        resource.setMeta(meta);
    }
}
