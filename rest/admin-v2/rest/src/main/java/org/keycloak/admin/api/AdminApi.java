package org.keycloak.admin.api;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.keycloak.admin.api.client.ClientsApi;

public interface AdminApi {

    String CONTENT_TYPE_MERGE_PATCH = "application/merge-patch+json";

    /**
     * Retrieve the Clients API group by version
     */
    @Path("clients/{version:v\\d+}")
    ClientsApi clients(@PathParam("version") String version);
}
