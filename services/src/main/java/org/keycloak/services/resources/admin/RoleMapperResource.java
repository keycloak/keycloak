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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.storage.ReadOnlyException;

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

/**
 * Base resource for managing users
 *
 * @resource Role Mapper
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:mpaulosnunes@gmail.com">Miguel P. Nunes</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class RoleMapperResource implements RoleMappingAwareResource {

    protected static final Logger logger = Logger.getLogger(RoleMapperResource.class);

    protected final RealmModel realm;

    private final RoleMapperModel roleMapper;

    private final AdminEventBuilder adminEvent;

    protected final AdminPermissionEvaluator.RequirePermissionCheck managePermission;
    protected final AdminPermissionEvaluator.RequirePermissionCheck viewPermission;
    private final AdminPermissionEvaluator auth;

    protected final ClientConnection clientConnection;

    protected final KeycloakSession session;

    protected final HttpHeaders headers;

    public RoleMapperResource(KeycloakSession session,
                              AdminPermissionEvaluator auth,
                              RoleMapperModel roleMapper,
                              AdminEventBuilder adminEvent,
                              AdminPermissionEvaluator.RequirePermissionCheck manageCheck,
                              AdminPermissionEvaluator.RequirePermissionCheck viewCheck) {
        this.session = session;
        this.auth = auth;
        this.realm = session.getContext().getRealm();
        this.clientConnection = session.getContext().getConnection();
        this.adminEvent = adminEvent.resource(ResourceType.REALM_ROLE_MAPPING);
        this.roleMapper = roleMapper;
        this.managePermission = manageCheck;
        this.viewPermission = viewCheck;
        this.headers = session.getContext().getRequestHeaders();

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
     * Get role mappings
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLE_MAPPER)
    @Operation(summary = "Get role mappings")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = MappingsRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public MappingsRepresentation getRoleMappings() {
        viewPermission.require();

        return ModelToRepresentation.toMappingsRepresentation(roleMapper.getRoleMappingsStream().filter(auth.roles()::canView));
    }

    @Path("composite")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLE_MAPPER)
    @Operation(summary = "Get effective role mappings",
        description = "This will recurse all composite roles, and for users also the groups they belong to, to get the result.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = MappingsRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public MappingsRepresentation getCompositeRoleMappings() {
        viewPermission.require();

        return ModelToRepresentation.toMappingsRepresentation(getEffectiveRoleMappings().stream().filter(auth.roles()::canView));
    }

    @Path("inherited")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLE_MAPPER)
    @Operation(summary = "Get inherited role mappings",
        description = "Returns the roles obtained through composite roles and, for users, through the groups they belong to. "
                + "A role that is both mapped directly and inherited is included.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = MappingsRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public MappingsRepresentation getInheritedRoleMappings() {
        viewPermission.require();

        Stream<RoleModel> groupRoles = getParentGroups().flatMap(GroupModel::getRoleMappingsStream);

        return ModelToRepresentation.toMappingsRepresentation(
                getInheritedRoles(roleMapper.getRoleMappingsStream(), groupRoles).filter(auth.roles()::canView));
    }

    // the groups the roles are inherited from: for a user all its groups and their parents, for a group its parents
    private Stream<GroupModel> getParentGroups() {
        Stream<GroupModel> groups;

        if (roleMapper instanceof GroupModel group) {
            groups = Stream.ofNullable(group.getParent());
        } else {
            groups = ((UserModel) roleMapper).getGroupsStream();
        }

        return groups.flatMap(g -> Stream.iterate(g, Objects::nonNull, GroupModel::getParent));
    }

    @Path("available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLE_MAPPER)
    @Operation(summary = "Get the roles that can still be mapped by the caller",
        description = "Returns the roles that are not yet mapped and that the caller is allowed to map. "
                + "Realm roles are returned in full, client roles can be paginated with first/max and filtered with search.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = MappingsRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public MappingsRepresentation getAvailableRoleMappings(
            @Parameter(description = "filter by role name or client id") @QueryParam("search") String search,
            @Parameter(description = "first client role to return") @QueryParam("first") Integer first,
            @Parameter(description = "maximum number of client roles to return") @QueryParam("max") Integer max) {
        viewPermission.require();

        Set<String> mapped = roleMapper.getRoleMappingsStream().map(RoleModel::getId).collect(Collectors.toSet());

        return getAvailableMappings(AdminPermissionsSchema.MAP_ROLE, search, mapped, first, max);
    }

    private Set<RoleModel> getEffectiveRoleMappings() {
        if (roleMapper instanceof GroupModel group) {
            // GroupModel.hasRole() (the semantics this endpoint previously relied on)
            // walks the parent-group chain, but RoleUtils.getDeepRoleMappings() only
            // expands a group's own direct mappings. Walk the chain explicitly here
            // to preserve parent-group role inheritance for the group endpoint.
            Set<RoleModel> directMappings = new HashSet<>();
            for (GroupModel current = group; current != null; current = current.getParent()) {
                directMappings.addAll(current.getRoleMappingsStream().collect(Collectors.toSet()));
            }
            return RoleUtils.expandCompositeRoles(directMappings);
        }
        return RoleUtils.getDeepRoleMappings(roleMapper);
    }

    /**
     * Get realm-level role mappings
     *
     * @return
     */
    @Path("realm")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLE_MAPPER)
    @Operation(summary = "Get realm-level role mappings")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = RoleRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<RoleRepresentation> getRealmRoleMappings() {
        viewPermission.require();

        return roleMapper.getRealmRoleMappingsStream()
                .filter(r -> auth.roles().canView(r))
                .map(ModelToRepresentation::toBriefRepresentation);
    }

    /**
     * Get effective realm-level role mappings
     *
     * This will recurse all composite roles to get the result.
     *
     * @param briefRepresentation if false, return roles with their attributes
     *
     * @return
     */
    @Path("realm/composite")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLE_MAPPER)
    @Operation(summary = "Get effective realm-level role mappings This will recurse all composite roles to get the result.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = RoleRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<RoleRepresentation> getCompositeRealmRoleMappings(@Parameter(description = "if false, return roles with their attributes") @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation) {
        viewPermission.require();
        Function<RoleModel, RoleRepresentation> toBriefRepresentation = briefRepresentation ?
                ModelToRepresentation::toBriefRepresentation : ModelToRepresentation::toRepresentation;
        return getEffectiveRoleMappings().stream()
                .filter(r -> RoleUtils.isRealmRole(r, realm))
                .filter(r -> auth.roles().canView(r))
                .map(toBriefRepresentation);
    }

    /**
     * Get realm-level roles that can be mapped
     *
     * @return
     */
    @Path("realm/available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLE_MAPPER)
    @Operation(summary = "Get realm-level roles that can be mapped")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = RoleRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<RoleRepresentation> getAvailableRealmRoleMappings() {
        viewPermission.require();

        return realm.getRolesStream()
                .filter(this::canMapRole)
                .filter(((Predicate<RoleModel>) roleMapper::hasDirectRole).negate())
                .map(ModelToRepresentation::toBriefRepresentation);
    }

    /**
     * Add realm-level role mappings to the user
     *
     * @param roles Roles to add
     */
    @Path("realm")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLE_MAPPER)
    @Operation(summary = "Add realm-level role mappings to the user")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found"),
        @APIResponse(responseCode = "500", description = "Internal Server Error")
    })
    public void addRealmRoleMappings(@Parameter(description = "Roles to add") List<RoleRepresentation> roles) {
        managePermission.require();

        logger.debugv("** addRealmRoleMappings: {0}", roles);

        try {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = realm.getRole(role.getName());
                if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                    throw new NotFoundException("Role not found");
                }
                auth.roles().requireMapRole(roleModel);
                roleMapper.grantRole(roleModel);
            }
        } catch (ModelIllegalStateException e) {
            logger.error(e.getMessage(), e);
            throw ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        } catch (ModelException | ReadOnlyException me) {
            logger.warn(me.getMessage(), me);
            throw new ErrorResponseException("invalid_request", "Could not add user role mappings!", Response.Status.BAD_REQUEST);
        }

        if (!roles.isEmpty()) {
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(roles).success();
        }
    }

    /**
     * Delete realm-level role mappings
     *
     * @param roles
     */
    @Path("realm")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROLE_MAPPER)
    @Operation(summary = "Delete realm-level role mappings")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found"),
        @APIResponse(responseCode = "500", description = "Internal Server Error")
    })
    public void deleteRealmRoleMappings(List<RoleRepresentation> roles) {
        managePermission.require();

        logger.debug("deleteRealmRoleMappings");
        if (roles == null) {
            roles = roleMapper.getRealmRoleMappingsStream()
                    .peek(roleModel -> {
                        auth.roles().requireMapRole(roleModel);
                        roleMapper.deleteRoleMapping(roleModel);
                    })
                    .map(ModelToRepresentation::toBriefRepresentation)
                    .collect(Collectors.toList());

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = realm.getRole(role.getName());
                if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                    throw new NotFoundException("Role not found");
                }
                auth.roles().requireMapRole(roleModel);
                try {
                    roleMapper.deleteRoleMapping(roleModel);
                } catch (ModelIllegalStateException e) {
                    logger.error(e.getMessage(), e);
                    throw ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
                } catch (ModelException | ReadOnlyException me) {
                    logger.warn(me.getMessage(), me);
                    throw new ErrorResponseException("invalid_request", "Could not remove user role mappings!", Response.Status.BAD_REQUEST);
                }
            }

        }

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(roles).success();

    }

    private boolean canMapRole(RoleModel roleModel) {
        return auth.roles().canMapRole(roleModel);
    }

    @Path("clients/{client-id}")
    public ClientRoleMappingsResource getUserClientRoleMappingsResource(@PathParam("client-id") @Parameter(description = "client id (not clientId!)") String client) {
        ClientModel clientModel = realm.getClientById(client);
        if (clientModel == null) {
            throw new NotFoundException("Client not found");
        }
        ClientRoleMappingsResource resource = new ClientRoleMappingsResource(session.getContext().getUri(), session, realm, auth, roleMapper,
                clientModel, adminEvent,
                managePermission, viewPermission);
        return resource;

    }
}
