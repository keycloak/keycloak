package org.keycloak.admin.api;

import jakarta.ws.rs.Path;

import org.keycloak.admin.api.client.ClientsApi;

public interface AdminApi {

    /**
     * Retrieve the Clients v2 api
     */
    @Path("clients/v2")
    ClientsApi clientsV2();
}
