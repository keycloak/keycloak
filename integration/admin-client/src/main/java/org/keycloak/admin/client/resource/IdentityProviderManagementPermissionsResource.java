package org.keycloak.admin.client.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.representations.idm.ManagementPermissionReference;

public interface IdentityProviderManagementPermissionsResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ManagementPermissionReference toReference();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void update(ManagementPermissionReference managementPermissionReference);
}
