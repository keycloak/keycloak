package org.keycloak.admin;

import jakarta.ws.rs.Path;
import org.keycloak.admin.api.AdminApi;
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
        return session.getProvider(RealmsApi.class);
    }

    @Override
    public void close() {

    }
}
