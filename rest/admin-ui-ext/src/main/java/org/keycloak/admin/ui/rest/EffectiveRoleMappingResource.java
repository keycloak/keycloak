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

import org.keycloak.admin.ui.rest.model.ClientRole;
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

import static org.keycloak.admin.ui.rest.model.RoleMapper.convertToModel;

public class EffectiveRoleMappingResource extends RoleMappingResource {
    public EffectiveRoleMappingResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        super(session, realm, auth);
    }

    @GET
    @Path("/clientScopes/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all effective roles for this client scope",
            description = "This endpoint returns all the client role mapping for a specific client scope"
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
    public final List<ClientRole> listCompositeClientScopeRoleMappings(@PathParam("id") String id) {
        ClientScopeModel clientScope = this.realm.getClientScopeById(id);
        if (clientScope == null) {
            throw new NotFoundException("Could not find client scope");
        }
        this.auth.clients().requireView(clientScope);
        return toSortedClientRoles(
                addSubClientRoles(clientScope.getScopeMappingsStream())
        );
    }

    @GET
    @Path("/clients/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all effective roles for this client",
            description = "This endpoint returns all the client role mapping for a specific client"
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
    public final List<ClientRole> listCompositeClientsRoleMappings(@PathParam("id") String id) {
        ClientModel client = this.realm.getClientById(id);
        if (client == null) {
            throw new NotFoundException("Could not find client");
        }

        auth.clients().requireView(client);
        return toSortedClientRoles(
                addSubClientRoles(client.getScopeMappingsStream())
        );
    }

    @GET
    @Path("/groups/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all effective roles for this group",
            description = "This endpoint returns all the client role mapping for a specific group"
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
    public final List<ClientRole> listCompositeGroupsRoleMappings(@PathParam("id") String id) {
        GroupModel group = this.realm.getGroupById(id);
        if (group == null) {
            throw new NotFoundException("Could not find group");
        }

        auth.groups().requireView(group);
        return toSortedClientRoles(
                addSubClientRoles(addParents(group).flatMap(GroupModel::getRoleMappingsStream))
        );
    }

    @GET
    @Path("/users/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all effective roles for this users",
            description = "This endpoint returns all the client role mapping for a specific users"
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
    public final List<ClientRole> listCompositeUsersRoleMappings(@PathParam("id") String id) {
        UserModel user = session.users().getUserById(this.realm, id);
        if (user == null) {
            if (auth.users().canQuery()) throw new NotFoundException("User not found");
            else throw new ForbiddenException();
        }
        auth.users().requireView(user);
        return toSortedClientRoles(
                addSubClientRoles(Stream.concat(
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
            summary = "List all effective roles for this realm role",
            description = "This endpoint returns all the client role mapping for a specific realm role"
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
    public final List<ClientRole> listCompositeRealmRoleMappings() {
        auth.roles().requireList(realm);
        final RoleModel defaultRole = this.realm.getDefaultRole();
        //this definitely does not return what the descriptions says
        return toSortedClientRoles(addSubClientRoles(Stream.of(defaultRole)));
    }

    private Stream<RoleModel> addSubClientRoles(Stream<RoleModel> roles) {
        return addSubRoles(roles).filter(RoleModel::isClientRole);
    }

    private List<ClientRole> toSortedClientRoles(Stream<RoleModel> roles) {
        return roles.map(roleModel -> convertToModel(roleModel, realm))
                .sorted(Comparator.comparing(ClientRole::getClient).thenComparing(ClientRole::getRole))
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
        //no cycle check here, I hope that's fine
        if (group.getParent() == null) {
            return Stream.of(group);
        }
        return Stream.concat(Stream.of(group), addParents(group.getParent()));
    }
}
