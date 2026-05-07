package org.keycloak.admin.ui.rest;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.keycloak.admin.ui.rest.model.EffectiveRole;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import static org.keycloak.admin.ui.rest.model.RoleMapper.convertToEffectiveRole;

public class AllEffectiveRoleMappingResource extends RoleMappingResource {
    public AllEffectiveRoleMappingResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        super(session, realm, auth);
    }

    @GET
    @Path("/clientScopes/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all effective roles (realm and client) for this client scope",
            description = "This endpoint returns all effective role mappings for a specific client scope"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = EffectiveRole.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final List<EffectiveRole> listAllEffectiveClientScopeRoleMappings(@PathParam("id") String id) {
        ClientScopeModel clientScope = this.realm.getClientScopeById(id);
        if (clientScope == null) {
            throw new NotFoundException("Could not find client scope");
        }
        this.auth.clients().requireView(clientScope);
        return toSortedEffectiveRoles(
                addSubRoles(clientScope.getScopeMappingsStream())
        );
    }

    @GET
    @Path("/clients/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all effective roles (realm and client) for this client",
            description = "This endpoint returns all effective role mappings for a specific client"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = EffectiveRole.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final List<EffectiveRole> listAllEffectiveClientsRoleMappings(@PathParam("id") String id) {
        ClientModel client = this.realm.getClientById(id);
        if (client == null) {
            throw new NotFoundException("Could not find client");
        }

        auth.clients().requireView(client);
        return toSortedEffectiveRoles(
                addSubRoles(client.getScopeMappingsStream())
        );
    }

    @GET
    @Path("/groups/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all effective roles (realm and client) for this group",
            description = "This endpoint returns all effective role mappings for a specific group"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = EffectiveRole.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final List<EffectiveRole> listAllEffectiveGroupsRoleMappings(@PathParam("id") String id) {
        GroupModel group = this.realm.getGroupById(id);
        if (group == null) {
            throw new NotFoundException("Could not find group");
        }

        auth.groups().requireView(group);
        return toSortedEffectiveRoles(
                addSubRoles(addParents(group).flatMap(GroupModel::getRoleMappingsStream))
        );
    }

    @GET
    @Path("/users/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all effective roles (realm and client) for this user",
            description = "This endpoint returns all effective role mappings for a specific user"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = EffectiveRole.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final List<EffectiveRole> listAllEffectiveUsersRoleMappings(@PathParam("id") String id) {
        UserModel user = session.users().getUserById(this.realm, id);
        if (user == null) {
            if (auth.users().canQuery()) throw new NotFoundException("User not found");
            else throw new ForbiddenException();
        }
        auth.users().requireView(user);
        return toSortedEffectiveRoles(
                addSubRoles(Stream.concat(
                        user.getRoleMappingsStream(),
                        user.getGroupsStream()
                                .flatMap(g -> addParents(g))
                                .flatMap(GroupModel::getRoleMappingsStream)))
        );
    }

    @GET
    @Path("/roles/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all effective roles (realm and client) for this composite role",
            description = "This endpoint returns all effective role mappings for a specific role"
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = EffectiveRole.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final List<EffectiveRole> listAllEffectiveRealmRoleMappings(@PathParam("id") String id) {
        RoleModel role = this.realm.getRoleById(id);
        if (role == null) {
            role = this.session.roles().getRoleById(this.realm, id);
        }
        if (role == null) {
            throw new NotFoundException("Could not find role");
        }

        auth.roles().requireView(role);
        return toSortedEffectiveRoles(addSubRoles(Stream.of(role)));
    }

    private List<EffectiveRole> toSortedEffectiveRoles(Stream<RoleModel> roles) {
        return roles.map(roleModel -> convertToEffectiveRole(roleModel, realm))
                .sorted(Comparator.comparing(EffectiveRole::isClientRole)
                        .thenComparing(r -> r.getClient() != null ? r.getClient() : "")
                        .thenComparing(EffectiveRole::getName))
                .collect(Collectors.toList());
    }

    private Stream<RoleModel> addSubRoles(Stream<RoleModel> roles) {
        return addSubRoles(roles, new HashSet<>());
    }

    private Stream<RoleModel> addSubRoles(Stream<RoleModel> roles, HashSet<RoleModel> visited) {
        List<RoleModel> roleList = roles.collect(Collectors.toList());
        visited.addAll(roleList);
        return Stream.concat(roleList.stream(), roleList.stream().flatMap(r -> addSubRoles(r.getCompositesStream().filter(s -> !visited.contains(s)), visited)));
    }

    private Stream<GroupModel> addParents(GroupModel group) {
        if (group.getParent() == null) {
            return Stream.of(group);
        }
        return Stream.concat(Stream.of(group), addParents(group.getParent()));
    }
}
