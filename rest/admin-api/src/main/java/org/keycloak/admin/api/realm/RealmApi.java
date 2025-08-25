package org.keycloak.admin.api.realm;

import jakarta.ws.rs.Path;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.provider.Provider;

public interface RealmApi extends Provider {

    @Path("clients")
    ClientsApi clients();
}
