package org.keycloak.scim.services.admin;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.component.ComponentValidationException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.scim.model.resourcetype.definition.ScimResourceTypeRepresentation;
import org.keycloak.scim.model.resourcetype.definition.ScimResourceTypeStore;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/**
 * Admin REST endpoint for managing custom SCIM resource type definitions of a realm.
 * <p>
 * Exposed under {@code /admin/realms/{realm}/scim/resource-types}. Definitions are stored as realm components and
 * validated through the component factory. Built-in resource types are returned as read-only entries so they can
 * be listed alongside custom ones.
 */
public class ScimResourceTypesAdminResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final ScimResourceTypeStore store;

    public ScimResourceTypesAdminResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth,
            AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.COMPONENT);
        this.store = new ScimResourceTypeStore(session, realm);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ScimResourceTypeRepresentation> getResourceTypes() {
        auth.realm().requireViewRealm();
        return store.getAllDefinitions().toList();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ScimResourceTypeRepresentation getResourceType(@PathParam("id") String id) {
        auth.realm().requireViewRealm();

        ScimResourceTypeRepresentation definition = store.getById(id);

        if (definition == null) {
            throw new NotFoundException("Could not find resource type");
        }

        return definition;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createResourceType(ScimResourceTypeRepresentation definition) {
        auth.realm().requireManageRealm();

        try {
            ScimResourceTypeRepresentation created = store.create(definition);

            adminEvent.operation(OperationType.CREATE)
                    .resourcePath(session.getContext().getUri(), created.getId())
                    .representation(created)
                    .success();

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(created.getId()).build())
                    .entity(created)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (ComponentValidationException e) {
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateResourceType(@PathParam("id") String id, ScimResourceTypeRepresentation definition) {
        auth.realm().requireManageRealm();

        try {
            ScimResourceTypeRepresentation updated = store.update(id, definition);

            if (updated == null) {
                throw new NotFoundException("Could not find resource type");
            }

            adminEvent.operation(OperationType.UPDATE)
                    .resourcePath(session.getContext().getUri())
                    .representation(updated)
                    .success();

            return Response.ok(updated, MediaType.APPLICATION_JSON).build();
        } catch (ComponentValidationException e) {
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @DELETE
    @Path("{id}")
    public Response deleteResourceType(@PathParam("id") String id) {
        auth.realm().requireManageRealm();

        if (!store.delete(id)) {
            throw new NotFoundException("Could not find resource type");
        }

        adminEvent.operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .success();

        return Response.noContent().build();
    }
}
