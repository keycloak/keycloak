package org.keycloak.admin.ui.rest;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import org.keycloak.models.UserProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class AvailableRoleMappingResource extends RoleMappingResource {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    public AvailableRoleMappingResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
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
            summary = "List all composite client roles for this client scope",
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
    public final List<ClientRole> listCompositeClientScopeRoleMappings(@PathParam("id") String id, @QueryParam("first")
        @DefaultValue("0") long first, @QueryParam("max") @DefaultValue("10") long max, @QueryParam("search") @DefaultValue("") String search) {
        ClientScopeModel scopeModel = this.realm.getClientScopeById(id);
        if (scopeModel == null) {
            throw new NotFoundException("Could not find client scope");
        } else {
            this.auth.clients().requireView(scopeModel);
            return this.mapping(((Predicate<RoleModel>) scopeModel::hasDirectScope).negate(), first, max, search);
        }
    }

    @GET
    @Path("/clients/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all composite client roles for this client",
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
    public final List<ClientRole> listCompositeClientRoleMappings(@PathParam("id")  String id, @QueryParam("first")
        @DefaultValue("0") long first, @QueryParam("max") @DefaultValue("10") long max, @QueryParam("search") @DefaultValue("") String search) {
        ClientModel client = this.realm.getClientById(id);
        if (client == null) {
            throw new NotFoundException("Could not find client");
        } else {
            this.auth.clients().requireView(client);
            return this.mapping(((Predicate<RoleModel>) client::hasDirectScope).negate(), first, max, search);
        }
    }

    @GET
    @Path("/groups/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all composite client roles for this group",
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
    public final List<ClientRole> listCompositeGroupRoleMappings(@PathParam("id")  String id, @QueryParam("first")
        @DefaultValue("0") long first, @QueryParam("max") @DefaultValue("10") long max, @QueryParam("search") @DefaultValue("") String search) {
        GroupModel group = this.realm.getGroupById(id);
        if (group == null) {
            throw new NotFoundException("Could not find group");
        } else {
            this.auth.groups().requireView(group);
            return this.mapping(((Predicate<RoleModel>) group::hasDirectRole).negate(), first, max, search);
        }
    }

    @GET
    @Path("/users/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all composite client roles for this user",
            description = "This endpoint returns all the client role mapping for a specific user"
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
    public final List<ClientRole> listCompositeUserRoleMappings(@PathParam("id") String id, @QueryParam("first") @DefaultValue("0") long first,
            @QueryParam("max") @DefaultValue("10") long max, @QueryParam("search") @DefaultValue("") String search) {
        UserProvider users = Objects.requireNonNull(session).users();
        UserModel userModel = users.getUserById(this.realm, id);
        if (userModel == null) {
            if (auth.users().canQuery()) throw new NotFoundException("User not found");
            else throw new ForbiddenException();
        }

        this.auth.users().requireView(userModel);
        return this.mapping(((Predicate<RoleModel>) userModel::hasDirectRole).negate(), first, max, search);
    }

    @GET
    @Path("/roles/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "List all composite client roles",
            description = "This endpoint returns all the client role"
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
    public final List<ClientRole> listCompositeRoleMappings(@QueryParam("first") @DefaultValue("0") long first,
            @QueryParam("max") @DefaultValue("10") long max, @QueryParam("search") @DefaultValue("") String search) {
        return this.mapping(o -> true, first, max, search);
    }
}
