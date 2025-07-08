package org.keycloak.admin.client.resource;

import jakarta.ws.rs.Path;

public interface IdentityProviderManagementResource {
    @Path("/permissions")
    IdentityProviderManagementPermissionsResource permissions();
}
