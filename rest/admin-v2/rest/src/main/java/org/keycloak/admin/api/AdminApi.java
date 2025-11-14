package org.keycloak.admin.api;

import jakarta.ws.rs.Path;

import org.keycloak.admin.api.realm.RealmsApi;

public interface AdminApi {

    @Path("realms")
    RealmsApi realms();
}
