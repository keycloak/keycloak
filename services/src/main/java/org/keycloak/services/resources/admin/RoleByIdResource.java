/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services.resources.admin;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissionManagement;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.utils.ProfileHelper;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import static org.keycloak.models.utils.ModelToRepresentation.toMappingsRepresentation;

/**
 * Sometimes its easier to just interact with roles by their ID instead of container/role-name
 *
 * @resource Roles (by ID)
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class RoleByIdResource extends RoleResource implements RoleMappingAwareResource {
    protected static final Logger logger = Logger.getLogger(RoleByIdResource.class);
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    private final KeycloakSession session;

    public RoleByIdResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        super(session.getContext().getRealm());
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public AdminPermissionEvaluator getAuth() {
        return auth;
    }

    /**
     * Get a specific role's representation
     *
     * @param id id of role
     * @return
     */
    @Path("{role-id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Get a specific role's representation")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = RoleRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public RoleRepresentation getRole(final @Parameter(description = "id of role") @PathParam("role-id") String id) {
        RoleModel roleModel = getRoleModel(id);
        auth.roles().requireView(roleModel);
        return getRole(roleModel);
    }

    protected RoleModel getRoleModel(String id) {
        RoleModel roleModel = realm.getRoleById(id);
        if (roleModel == null) {
            throw new NotFoundException("Could not find role with id");
        }
       return roleModel;
    }

    /**
     * Delete the role
     *
     * @param id id of role
     */
    @Path("{role-id}")
    @DELETE
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Delete the role")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public void deleteRole(final @Parameter(description = "id of role") @PathParam("role-id") String id) {
        if (realm.getDefaultRole() == null) {
            logger.warnf("Default role for realm with id '%s' doesn't exist.", realm.getId());
        } else if (realm.getDefaultRole().getId().equals(id)) {
            throw ErrorResponse.error(realm.getDefaultRole().getName() + " is default role of the realm and cannot be removed.",
                    Response.Status.BAD_REQUEST);
        }

        RoleModel role = getRoleModel(id);
        auth.roles().requireManage(role);
        deleteRole(role);

        if (role.isClientRole()) {
            adminEvent.resource(ResourceType.CLIENT_ROLE);
        } else {
            adminEvent.resource(ResourceType.REALM_ROLE);
        }

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Update the role
     *
     * @param id id of role
     * @param rep
     */
    @Path("{role-id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Update the role")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public void updateRole(final @Parameter(description = "id of role") @PathParam("role-id") String id, final RoleRepresentation rep) {
        RoleModel role = getRoleModel(id);
        auth.roles().requireManage(role);
        updateRole(rep, role, realm, session);

        if (role.isClientRole()) {
            adminEvent.resource(ResourceType.CLIENT_ROLE);
        } else {
            adminEvent.resource(ResourceType.REALM_ROLE);
        }

        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
    }

    /**
     * Make the role a composite role by associating some child roles
     *
     * @param id
     * @param roles
     */
    @Path("{role-id}/composites")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Make the role a composite role by associating some child roles")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public void addComposites(final @PathParam("role-id") String id, List<RoleRepresentation> roles) {
        // any role by ID
        RoleModel role = getRoleModel(id);
        auth.roles().requireManage(role);
        addComposites(auth, adminEvent, session.getContext().getUri(), roles, role);
    }

    /**
     * Get role's children
     *
     * Returns a set of role's children provided the role is a composite.
     *
     * @param id
     * @return
     */
    @Path("{role-id}/composites")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Get role's children Returns a set of role's children provided the role is a composite.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = RoleRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<RoleRepresentation> getRoleComposites(final @PathParam("role-id") String id,
                                                        final @QueryParam("search") String search,
                                                        final @QueryParam("first") Integer first,
                                                        final @QueryParam("max") Integer max
    ) {

        logger.debugf("*** getRoleComposites: '%s'", id);
        RoleModel role = getRoleModel(id);
        auth.roles().requireView(role);

        return paginate(role.getCompositesStream(search, null, null).filter(auth.roles()::canView), first, max)
                .map(ModelToRepresentation::toBriefRepresentation);
    }

    @Path("{role-id}/composites/composite")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Get role's effective children",
        description = "This will recurse all composite roles to get the result, grouped by realm and client.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = MappingsRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public MappingsRepresentation getCompositeRoleComposites(final @PathParam("role-id") String id) {
        RoleModel role = getRoleModel(id);
        auth.roles().requireView(role);

        return toMappingsRepresentation(RoleUtils.expandCompositeRolesStream(role.getCompositesStream())
                .filter(r -> !r.equals(role))
                .filter(auth.roles()::canView));
    }

    @Path("{role-id}/composites/inherited")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Get role's inherited children",
        description = "Returns the roles obtained through the composite children of the role, grouped by realm and client. "
                + "A role that is both a direct child and inherited is included.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = MappingsRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public MappingsRepresentation getInheritedRoleComposites(final @PathParam("role-id") String id) {
        RoleModel role = getRoleModel(id);
        auth.roles().requireView(role);

        return toMappingsRepresentation(getInheritedRoles(role.getCompositesStream(), Stream.empty())
                .filter(r -> !r.equals(role))
                .filter(auth.roles()::canView));
    }

    @Path("{role-id}/composites/available")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Get the roles that can still be added as children of the role by the caller",
        description = "Returns the realm and client roles that are not yet children of the role and that the caller is allowed to add as composite. "
                + "Pagination applies to client roles only.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = MappingsRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public MappingsRepresentation getAvailableRoleComposites(final @PathParam("role-id") String id,
                                                             final @Parameter(description = "filter by role name or client id") @QueryParam("search") String search,
                                                             final @Parameter(description = "first client role to return") @QueryParam("first") Integer first,
                                                             final @Parameter(description = "maximum number of client roles to return") @QueryParam("max") Integer max
    ) {
        RoleModel role = getRoleModel(id);
        auth.roles().requireView(role);

        Set<String> excluded = Stream.concat(Stream.of(role.getId()), role.getCompositesStream().map(RoleModel::getId))
                .collect(Collectors.toSet());

        return getAvailableMappings(AdminPermissionsSchema.MAP_ROLE_COMPOSITE, search, excluded, first, max);
    }

    private static Stream<RoleModel> paginate(Stream<RoleModel> roles, Integer first, Integer max) {
        if (first != null && first > 0) {
            roles = roles.skip(first);
        }
        if (max != null && max >= 0) {
            roles = roles.limit(max);
        }
        return roles;
    }

    /**
     * Get realm-level roles that are in the role's composite
     *
     * @param id
     * @return
     */
    @Path("{role-id}/composites/realm")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Get realm-level roles that are in the role's composite")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = RoleRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<RoleRepresentation> getRealmRoleComposites(final @PathParam("role-id") String id) {
        RoleModel role = getRoleModel(id);
        auth.roles().requireView(role);
        return getRealmRoleComposites(auth, role);
    }

    /**
     * Get client-level roles for the client that are in the role's composite
     *
     * @param id
     * @param clientUuid
     * @return
     */
    @Path("{role-id}/composites/clients/{clientUuid}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Get client-level roles for the client that are in the role's composite")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = RoleRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public Stream<RoleRepresentation> getClientRoleComposites(final @PathParam("role-id") String id,
                                                                final @PathParam("clientUuid") String clientUuid) {

        RoleModel role = getRoleModel(id);
        auth.roles().requireView(role);
        ClientModel clientModel = realm.getClientById(clientUuid);
        if (clientModel == null) {
            throw new NotFoundException("Could not find client");
        }
        return getClientRoleComposites(auth, clientModel, role);
    }

    /**
     * Remove a set of roles from the role's composite
     *
     * @param id Role id
     * @param roles A set of roles to be removed
     */
    @Path("{role-id}/composites")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Remove a set of roles from the role's composite")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public void deleteComposites(final @Parameter(description = "Role id") @PathParam("role-id") String id,
                                 @Parameter(description = "A set of roles to be removed") List<RoleRepresentation> roles) {
        RoleModel role = getRoleModel(id);
        auth.roles().requireManage(role);
        deleteComposites(auth, adminEvent, session.getContext().getUri(), roles, role);
    }

    /**
     * Return object stating whether role Authorization permissions have been initialized or not and a reference
     *
     *
     * @param id
     * @return
     */
    @Path("{role-id}/management/permissions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Return object stating whether role Authorization permissions have been initialized or not and a reference")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = ManagementPermissionReference.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public ManagementPermissionReference getManagementPermissions(final @PathParam("role-id") String id) {
        ProfileHelper.requireFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        RoleModel role = getRoleModel(id);
        auth.roles().requireView(role.getContainer());

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (!permissions.roles().isPermissionsEnabled(role)) {
            return new ManagementPermissionReference();
        }
        return toMgmtRef(role, permissions);
    }

    public static ManagementPermissionReference toMgmtRef(RoleModel role, AdminPermissionManagement permissions) {
        ManagementPermissionReference ref = new ManagementPermissionReference();
        ref.setEnabled(true);
        ref.setResource(permissions.roles().resource(role).getId());
        ref.setScopePermissions(permissions.roles().getPermissions(role));
        return ref;
    }

    /**
     * Return object stating whether role Authorization permissions have been initialized or not and a reference
     *
     *
     * @param id
     * @return initialized manage permissions reference
     */
    @Path("{role-id}/management/permissions")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLES_BY_ID)
    @Operation(summary = "Return object stating whether role Authorization permissions have been initialized or not and a reference")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = ManagementPermissionReference.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public ManagementPermissionReference setManagementPermissionsEnabled(final @PathParam("role-id") String id, ManagementPermissionReference ref) {
        ProfileHelper.requireFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        RoleModel role = getRoleModel(id);
        auth.roles().requireManage(role);

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        permissions.roles().setPermissionsEnabled(role, ref.isEnabled());
        if (ref.isEnabled()) {
            return toMgmtRef(role, permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }

}
