package org.keycloak.admin.api;

import jakarta.ws.rs.Path;
import org.keycloak.admin.api.realm.RealmsApi;
import org.keycloak.provider.Provider;

public interface AdminApi extends Provider {

    @Path("realms")
    RealmsApi realms();
}
