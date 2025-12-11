package org.keycloak.admin.api.client;

import jakarta.ws.rs.Path;

/**
 * API group for handling all client-related resources
 */
public interface ClientsApiGroup {

    /**
     * Manage clients for the realm
     */
    @Path("clients")
    ClientsApi clients();
}
