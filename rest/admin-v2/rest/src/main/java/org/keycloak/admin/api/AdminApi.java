package org.keycloak.admin.api;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.keycloak.admin.api.client.ClientsApiGroup;

public interface AdminApi {

    String CONTENT_TYPE_MERGE_PATCH = "application/merge-patch+json";

    /**
     * Retrieve the default Clients API group
     * <p>
     * It is recommended to always specify the version of the API to prevent consistency issues.
     * Use the default only for better developer experience.
     */
    @Path("clients-api")
    ClientsApiGroup clientsGroupDefault();

    /**
     * Retrieve the Clients API group by version
     */
    @Path("clients-api/{version:v\\d+}")
    ClientsApiGroup clientsGroup(@PathParam("version") String version);
}
