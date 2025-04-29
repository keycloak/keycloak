package org.keycloak.admin.api.realm;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.admin.api.client.DefaultClientsApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

public class DefaultRealmApi implements RealmApi {
    private final KeycloakSession session;
    private final RealmModel realm;

    public DefaultRealmApi(KeycloakSession session, String name) {
        this.session = session;
        this.realm = Optional.ofNullable(session.realms().getRealmByName(name)).orElseThrow(() -> new NotFoundException("Realm cannot be found"));
        session.getContext().setRealm(realm);
    }

    @Path("clients")
    @Override
    public ClientsApi clients() {
        return new DefaultClientsApi(session, realm);
    }
}
