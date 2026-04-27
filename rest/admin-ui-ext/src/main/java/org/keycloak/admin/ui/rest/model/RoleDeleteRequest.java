package org.keycloak.admin.ui.rest.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class RoleDeleteRequest {
    @Schema(required = true)
    private String roleId;

    @Schema(required = true)
    private String roleName;

    @Schema(description = "Client ID if this is a client role, null for realm roles")
    private String clientId;

    public RoleDeleteRequest() {
    }

    public RoleDeleteRequest(String roleId, String roleName, String clientId) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.clientId = clientId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
