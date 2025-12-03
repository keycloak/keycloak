package org.keycloak.admin.api;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.admin.api.client.ClientsApiGroup;
import org.keycloak.admin.api.client.DefaultClientsApiGroup;
import org.keycloak.models.KeycloakSession;

@Provider
@Path("admin/api")
public class AdminApiRootV2 {

    public static final String CONTENT_TYPE_MERGE_PATCH = "application/merge-patch+json";

    @Context
    protected KeycloakSession session;

    @Path("clients/v2")
    public ClientsApiGroup clientsApiGroup() {
        return new DefaultClientsApiGroup(session);
    }
}
