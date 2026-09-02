package org.keycloak.admin.ui.rest;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.keycloak.admin.ui.rest.model.ClientRole;
import org.keycloak.admin.ui.rest.model.RoleMapper;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES_CLIENT_SCOPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES_COMPOSITE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE_CLIENT_SCOPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE_COMPOSITE;

public class AvailableRoleMappingResource extends RoleMappingResource {
    public AvailableRoleMappingResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        super(session, realm, auth);
    }

    @GET
    @Path("/clientScopes/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all available client roles for this client scope",
            description = "This endpoint returns all the client roles the user can add to a specific client scope"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = ClientRole.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final List<ClientRole> listAvailableClientScopeRoleMappings(@PathParam("id") String id, @QueryParam("first")
        @DefaultValue("0") int first, @QueryParam("max") @DefaultValue("10") int max, @QueryParam("search") @DefaultValue("") String search) {
        ClientScopeModel scopeModel = this.realm.getClientScopeById(id);
        if (scopeModel == null) {
            if (auth.clients().canListClientScopes()) throw new NotFoundException("Could not find client scope");
            else throw new ForbiddenException();
        } else {
            if (auth.hasOneAdminRole(AdminRoles.MANAGE_CLIENTS)) {
                Stream<String> excludedRoleIds = scopeModel.getScopeMappingsStream().filter(RoleModel::isClientRole).map(RoleModel::getId);
                return searchForClientRolesByExcludedIds(realm, search, first, max, excludedRoleIds);
            }
            if (AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm) || Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
                auth.clients().requireView(scopeModel);
                Set<String> roleIds = getRoleIdsWithPermissions(MAP_ROLE_CLIENT_SCOPE, MAP_ROLES_CLIENT_SCOPE);
                scopeModel.getScopeMappingsStream().forEach(role -> roleIds.remove(role.getId()));
                return searchForClientRolesByIds(realm, roleIds.stream(), search, first, max);
            }
            return Collections.emptyList();
        }
    }

    @GET
    @Path("/clients/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all available client roles for the scope mapping of this client",
            description = "This endpoint returns all the client roles a user can add to the scope mapping of a specific client"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = ClientRole.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final List<ClientRole> listAvailableClientRoleMappings(@PathParam("id")  String id, @QueryParam("first")
        @DefaultValue("0") int first, @QueryParam("max") @DefaultValue("10") int max, @QueryParam("search") @DefaultValue("") String search) {
        ClientModel client = this.realm.getClientById(id);
        if (client == null) {
            if (auth.clients().canList()) throw new NotFoundException("Could not find client");
            else throw new ForbiddenException();
        } else {
            if (auth.hasOneAdminRole(AdminRoles.MANAGE_CLIENTS)) {
                Stream<String> excludedRoleIds = Stream.concat(client.getScopeMappingsStream(), client.getRolesStream()).filter(RoleModel::isClientRole).map(RoleModel::getId);
                return searchForClientRolesByExcludedIds(realm, search, first, max, excludedRoleIds);
            }
            if (AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm) || Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
                auth.clients().requireView(client);
                Set<String> roleIds = getRoleIdsWithPermissions(MAP_ROLE_CLIENT_SCOPE, MAP_ROLES_CLIENT_SCOPE);
                Stream.concat(client.getScopeMappingsStream(), client.getRolesStream()).forEach(role -> roleIds.remove(role.getId()));
                return searchForClientRolesByIds(realm, roleIds.stream(), search, first, max);
            }
            return Collections.emptyList();
        }
    }

    @GET
    @Path("/groups/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all available client roles for this group",
            description = "This endpoint returns all available client roles a user can add to a specific group"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = ClientRole.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final List<ClientRole> listAvailableGroupRoleMappings(@PathParam("id")  String id, @QueryParam("first")
        @DefaultValue("0") int first, @QueryParam("max") @DefaultValue("10") int max, @QueryParam("search") @DefaultValue("") String search) {
        GroupModel group = this.realm.getGroupById(id);
        if (group == null) {
            if (auth.groups().canList()) throw new NotFoundException("Could not find group");
            else throw new ForbiddenException();
        } else {
            if (auth.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
                Stream<String> excludedRoleIds = group.getRoleMappingsStream().filter(RoleModel::isClientRole).map(RoleModel::getId);
                return searchForClientRolesByExcludedIds(realm, search, first, max, excludedRoleIds);
            }
            if (AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm) || Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
                auth.groups().requireView(group);
                Set<String> roleIds = getRoleIdsWithPermissions(MAP_ROLE, MAP_ROLES);
                group.getRoleMappingsStream().forEach(role -> roleIds.remove(role.getId()));
                return searchForClientRolesByIds(realm, roleIds.stream(), search, first, max);
            }
            return Collections.emptyList();
        }
    }

    @GET
    @Path("/users/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all available client roles for this user",
            description = "This endpoint returns all the available client roles a user can add to a specific user"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = ClientRole.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final List<ClientRole> listAvailableUserRoleMappings(@PathParam("id") String id, @QueryParam("first") @DefaultValue("0") int first,
            @QueryParam("max") @DefaultValue("10") int max, @QueryParam("search") @DefaultValue("") String search) {
        UserProvider users = Objects.requireNonNull(session).users();
        UserModel userModel = users.getUserById(this.realm, id);
        if (userModel == null) {
            if (auth.users().canQuery()) throw new NotFoundException("User not found");
            else throw new ForbiddenException();
        } else {
            if (auth.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
                Stream<String> excludedRoleIds = userModel.getRoleMappingsStream().filter(RoleModel::isClientRole).map(RoleModel::getId);
                return searchForClientRolesByExcludedIds(realm, search, first, max, excludedRoleIds);
            }
            if (AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm) || Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
                auth.users().requireView(userModel);
                if (!auth.users().canMapRoles(userModel)) {
                    return Collections.emptyList();
                }
                Set<String> roleIds = getRoleIdsWithPermissions(MAP_ROLE, MAP_ROLES);
                userModel.getRoleMappingsStream().forEach(role -> roleIds.remove(role.getId()));
                return searchForClientRolesByIds(realm, roleIds.stream(), search, first, max);
            }
            return Collections.emptyList();
        }
    }

    @GET
    @Path("/roles/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all available client roles to map as composite role",
            description = "This endpoint returns all available client roles to map as composite role"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = ClientRole.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final List<ClientRole> listAvailableRoleMappings(@PathParam("id") String id, @QueryParam("first") @DefaultValue("0") int first,
            @QueryParam("max") @DefaultValue("10") int max, @QueryParam("search") @DefaultValue("") String search) {
        if (auth.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return searchForClientRolesByExcludedIds(realm, search, first, max, Stream.of(id));
        }
        if (AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm) || Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
            Set<String> roleIds = getRoleIdsWithPermissions(MAP_ROLE_COMPOSITE, MAP_ROLES_COMPOSITE);
            roleIds.remove(id);
            return searchForClientRolesByIds(realm, roleIds.stream(), search, first, max);
        }
        return Collections.emptyList();
    }

    private Set<String> getRoleIdsWithPermissions(String roleResourceScope, String clientResourceScope) {
        Set<String> roleIds;
        if (AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm) && canPerformOnAllClients(clientResourceScope)) {
            roleIds = session.clients().getClientsStream(realm).flatMap(client -> client.getRolesStream()).map(RoleModel::getId).collect(Collectors.toSet());
        } else {
            roleIds = this.auth.roles().getRoleIdsByScope(roleResourceScope);
            Set<String> clientIds = this.auth.clients().getClientIdsByScope(clientResourceScope);
            clientIds.stream().flatMap(cid -> realm.getClientById(cid).getRolesStream()).forEach(role -> roleIds.add(role.getId()));
        }
        return roleIds;
    }

    private List<ClientRole> searchForClientRolesByIds(RealmModel realm, Stream<String> includedIDs, String search, int first, int max) {
        Stream<RoleModel> result = session.roles().searchForClientRolesStream(realm, includedIDs, search, first, max);
        return result.map(role -> RoleMapper.convertToModel(role, realm)).collect(Collectors.toList());
    }

    private List<ClientRole> searchForClientRolesByExcludedIds(RealmModel realm, String search, int first, int max, Stream<String> excludedIds) {
        Stream<RoleModel> result = session.roles().searchForClientRolesStream(realm, search, excludedIds, first, max);
        return result.map(role -> RoleMapper.convertToModel(role, realm)).collect(Collectors.toList());
    }

    private boolean canPerformOnAllClients(String scope) {
        switch (scope) {
            case MAP_ROLES:
                return auth.clients().canMapRoles(null);
            case MAP_ROLES_COMPOSITE:
                return auth.clients().canMapCompositeRoles(null);
            case MAP_ROLES_CLIENT_SCOPE:
                return auth.clients().canMapClientScopeRoles(null);
            default:
                return false;
        }
    }
}
