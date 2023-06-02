package org.keycloak.admin.ui.rest;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.keycloak.admin.ui.rest.model.ClientRole;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class EffectiveRoleMappingResource extends RoleMappingResource {
    private KeycloakSession session;
    private RealmModel realm;
    private AdminPermissionEvaluator auth;

    public EffectiveRoleMappingResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        super(realm, auth);
        this.realm = realm;
        this.auth = auth;
        this.session = session;
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
        return this.mapping(clientScope::hasScope, auth.roles()::canMapClientScope).collect(Collectors.toList());
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
        return mapping(client::hasScope).collect(Collectors.toList());
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
        return mapping(group::hasRole).collect(Collectors.toList());
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
        return mapping(user::hasRole).collect(Collectors.toList());
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
        return mapping(o -> o.hasRole(defaultRole)).collect(Collectors.toList());
    }

}
