package org.keycloak.admin.api.realm;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.keycloak.models.KeycloakSession;

public class DefaultRealmsApi implements RealmsApi {
    private final KeycloakSession session;

    public DefaultRealmsApi(KeycloakSession session) {
        this.session = session;
    }

    @Path("{name}")
    @Override
    public RealmApi realm(@PathParam("name") String name) {
        return new DefaultRealmApi(session, name);
    }

    @Override
    public void close() {

    }
}
