package org.keycloak.admin.ui.rest;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.keycloak.admin.ui.rest.model.RoleDeleteRequest;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

public class RoleMappingDeleteResource {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public RoleMappingDeleteResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    @POST
    @Path("/groups/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "Delete role mappings from a group",
            description = "Deletes multiple role mappings from a group in a single request"
    )
    @APIResponse(responseCode = "204", description = "Role mappings deleted successfully")
    public void deleteGroupRoleMappings(@PathParam("id") String id, List<RoleDeleteRequest> roles) {
        GroupModel group = this.realm.getGroupById(id);
        if (group == null) {
            throw new NotFoundException("Could not find group");
        }
        auth.groups().requireManageMembership(group);

        deleteRoleMappings(roles, role -> group.deleteRoleMapping(role));

        adminEvent.operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .resource(ResourceType.REALM_ROLE_MAPPING)
                .success();
    }

    @POST
    @Path("/users/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "Delete role mappings from a user",
            description = "Deletes multiple role mappings from a user in a single request"
    )
    @APIResponse(responseCode = "204", description = "Role mappings deleted successfully")
    public void deleteUserRoleMappings(@PathParam("id") String id, List<RoleDeleteRequest> roles) {
        UserModel user = session.users().getUserById(this.realm, id);
        if (user == null) {
            if (auth.users().canQuery()) throw new NotFoundException("User not found");
            else throw new ForbiddenException();
        }
        auth.users().requireMapRoles(user);

        deleteRoleMappings(roles, role -> user.deleteRoleMapping(role));

        adminEvent.operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .resource(ResourceType.REALM_ROLE_MAPPING)
                .success();
    }

    @POST
    @Path("/clientScopes/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "Delete scope mappings from a client scope",
            description = "Deletes multiple scope mappings from a client scope in a single request"
    )
    @APIResponse(responseCode = "204", description = "Scope mappings deleted successfully")
    public void deleteClientScopeRoleMappings(@PathParam("id") String id, List<RoleDeleteRequest> roles) {
        ClientScopeModel clientScope = this.realm.getClientScopeById(id);
        if (clientScope == null) {
            throw new NotFoundException("Could not find client scope");
        }
        auth.clients().requireManage(clientScope);

        deleteRoleMappings(roles, role -> clientScope.deleteScopeMapping(role));

        adminEvent.operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .resource(ResourceType.CLIENT_SCOPE_MAPPING)
                .success();
    }

    @POST
    @Path("/clients/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "Delete scope mappings from a client",
            description = "Deletes multiple scope mappings from a client in a single request"
    )
    @APIResponse(responseCode = "204", description = "Scope mappings deleted successfully")
    public void deleteClientRoleMappings(@PathParam("id") String id, List<RoleDeleteRequest> roles) {
        ClientModel client = this.realm.getClientById(id);
        if (client == null) {
            throw new NotFoundException("Could not find client");
        }
        auth.clients().requireManage(client);

        deleteRoleMappings(roles, role -> client.deleteScopeMapping(role));

        adminEvent.operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .resource(ResourceType.CLIENT_SCOPE_MAPPING)
                .success();
    }

    @POST
    @Path("/roles/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "Delete composite roles from a role",
            description = "Deletes multiple composite roles from a role in a single request"
    )
    @APIResponse(responseCode = "204", description = "Composite roles deleted successfully")
    public void deleteCompositeRoles(@PathParam("id") String id, List<RoleDeleteRequest> roles) {
        RoleModel role = this.realm.getRoleById(id);
        if (role == null) {
            role = this.session.roles().getRoleById(this.realm, id);
        }
        if (role == null) {
            throw new NotFoundException("Could not find role");
        }
        auth.roles().requireManage(role);

        final RoleModel parentRole = role;
        deleteRoleMappings(roles, compositeRole -> parentRole.removeCompositeRole(compositeRole));

        adminEvent.operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .resource(ResourceType.REALM_ROLE)
                .success();
    }

    private void deleteRoleMappings(List<RoleDeleteRequest> roles, java.util.function.Consumer<RoleModel> deleteAction) {
        for (RoleDeleteRequest roleRequest : roles) {
            RoleModel role = this.realm.getRoleById(roleRequest.getRoleId());
            if (role == null) {
                role = this.session.roles().getRoleById(this.realm, roleRequest.getRoleId());
            }
            if (role != null) {
                deleteAction.accept(role);
            }
        }
    }
}
