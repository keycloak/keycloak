package org.keycloak.admin.ui.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.keycloak.admin.ui.rest.model.RoleMappingRepresentation;
import org.keycloak.admin.ui.rest.model.RoleMappingRepresentation.ClientMappingRepresentation;
import org.keycloak.admin.ui.rest.model.RoleMappingRepresentation.RoleRepresentation;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

public class RoleCompositeResource {
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AdminPermissionEvaluator auth;

    public RoleCompositeResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
    }

    @GET
    @Path("/roles/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "Get composite role mappings for a role",
            description = "Returns composite roles grouped by realm and client with client details resolved"
    )
    @APIResponse(
            responseCode = "200",
            description = "Role mappings",
            content = {@Content(
                    schema = @Schema(implementation = RoleMappingRepresentation.class)
            )}
    )
    public RoleMappingRepresentation getCompositeRoleMappings(@PathParam("id") String id) {
        RoleModel role = this.realm.getRoleById(id);
        if (role == null) {
            role = this.session.roles().getRoleById(this.realm, id);
        }
        if (role == null) {
            throw new NotFoundException("Could not find role");
        }

        auth.roles().requireView(role);

        List<RoleModel> compositeRoles = role.getCompositesStream().collect(Collectors.toList());

        List<RoleRepresentation> realmMappings = new ArrayList<>();
        Map<String, ClientMappingRepresentation> clientMappings = new HashMap<>();

        for (RoleModel compositeRole : compositeRoles) {
            RoleRepresentation roleRep = toRoleRepresentation(compositeRole);

            if (compositeRole.isClientRole()) {
                ClientModel client = this.realm.getClientById(compositeRole.getContainerId());
                if (client != null) {
                    String clientId = client.getClientId();
                    roleRep.setContainerId(clientId);

                    ClientMappingRepresentation clientMapping = clientMappings.get(clientId);
                    if (clientMapping == null) {
                        clientMapping = new ClientMappingRepresentation(client.getId(), clientId, new ArrayList<>());
                        clientMappings.put(clientId, clientMapping);
                    }
                    clientMapping.getMappings().add(roleRep);
                }
            } else {
                realmMappings.add(roleRep);
            }
        }

        return new RoleMappingRepresentation(
                realmMappings.isEmpty() ? null : realmMappings,
                clientMappings.isEmpty() ? null : clientMappings
        );
    }

    private RoleRepresentation toRoleRepresentation(RoleModel role) {
        return new RoleRepresentation(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isComposite(),
                role.isClientRole(),
                role.getContainerId()
        );
    }
}
