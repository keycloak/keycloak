package org.keycloak.admin.api;

import jakarta.ws.rs.Path;
import org.keycloak.admin.api.realm.DefaultRealmsApi;
import org.keycloak.admin.api.realm.RealmsApi;
import org.keycloak.models.KeycloakSession;

public class DefaultAdminApi implements AdminApi {
    private final KeycloakSession session;

    public DefaultAdminApi(KeycloakSession session) {
        this.session = session;
    }

    @Path("realms")
    @Override
    public RealmsApi realms() {
        return new DefaultRealmsApi(session);
    }

    @Override
    public void close() {

    }
}
