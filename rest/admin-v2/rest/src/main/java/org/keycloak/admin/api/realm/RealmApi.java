package org.keycloak.admin.api.realm;

import jakarta.ws.rs.Path;

import org.keycloak.admin.api.client.ClientsApi;

public interface RealmApi {

    @Path("clients")
    ClientsApi clients();
}
