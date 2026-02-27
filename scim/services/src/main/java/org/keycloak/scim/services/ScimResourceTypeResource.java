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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.common.Meta;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;
import org.keycloak.scim.resource.spi.SingletonResourceTypeProvider;
import org.keycloak.theme.Theme;
import org.keycloak.util.JsonSerialization;

public class ScimResourceTypeResource<R extends ResourceTypeRepresentation> {

    private static final String APPLICATION_SCIM_JSON = "application/scim+json";

    private final KeycloakSession session;
    private final ScimResourceTypeProvider<R> resourceTypeProvider;
    private final Class<? extends ResourceTypeRepresentation> resourceTypeClazz;

    public ScimResourceTypeResource(KeycloakSession session, ScimResourceTypeProvider<R> resourceTypeProvider) {
        this.session = session;
        this.resourceTypeProvider = resourceTypeProvider;
        this.resourceTypeClazz = resourceTypeProvider.getResourceType();
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
                (rScimResourceTypeProvider, r) -> resourceTypeProvider.create(r));
    }

    @Path("{id}")
    @GET
    @Produces(APPLICATION_SCIM_JSON)
    public Response get(@PathParam("id") String id) {
        R resource = getResource(id);

        if (resource == null) {
            return resourceNotFound(id);
        }

        setMetadata(resource, resource.getCreatedTimestamp());

        return Response.ok().entity(resource).build();
    }

    @GET
    @Produces(APPLICATION_SCIM_JSON)
    public Response getAll() {
        Stream<R> stream = resourceTypeProvider.getAll().peek((r ->  setMetadata(r, r.getCreatedTimestamp())));

        if (resourceTypeProvider instanceof SingletonResourceTypeProvider<R>) {
            return Response.ok().entity(stream
                            .peek(r -> setMetadata(r, r.getCreatedTimestamp()))
                            .findAny().orElseThrow(NotFoundException::new))
                    .build();
        }

        List<R> resources = stream.toList();

        ListResponse<R> response = new ListResponse<>();

        response.setResources(resources);

        // TODO: need to implement pagination and filtering, for now we just return all resources and set totalResults accordingly
        response.setTotalResults(response.getResources().size());

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

        return onPersist(resource, Status.OK,
                (rScimResourceTypeProvider, r) -> resourceTypeProvider.update(r));
    }

    private R parseResourceTypePayload(InputStream is) {
        try {
            return  (R) JsonSerialization.readValue(is, resourceTypeClazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize request body", e);
        }
    }

    private void setMetadata(R resource, long createdTimestamp) {
        Meta meta = new Meta();
        meta.setResourceType(resourceTypeProvider.getName());
        meta.setCreated(Instant.ofEpochMilli(createdTimestamp).toString());
        meta.setLastModified(meta.getCreated());
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

            setMetadata(resource, Time.currentTimeMillis());

            return Response.status(status).entity(resource).build();
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

    private Response errorResponse(Status status, String message) {
        return Response.status(status).entity(new ErrorResponse(message, status.getStatusCode())).build();
    }
}
