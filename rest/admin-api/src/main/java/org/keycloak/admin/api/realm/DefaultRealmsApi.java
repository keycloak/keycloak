package org.keycloak.admin.api.realm;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

public class DefaultRealmsApi implements RealmsApi {
    private final KeycloakSession session;

    public DefaultRealmsApi(KeycloakSession session) {
        this.session = session;
    }

    @Path("{name}")
    @Override
    public RealmApi realm(@PathParam("name") String name) {
        var realm = Optional.ofNullable(session.realms().getRealmByName(name)).orElseThrow(() -> new NotFoundException("Realm cannot be found"));
        session.getContext().setRealm(realm);
        return session.getProvider(RealmApi.class);
    }

    @Override
    public void close() {

    }
}
