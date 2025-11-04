package org.keycloak.admin.api.realm;

import jakarta.ws.rs.Path;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Objects;

public class DefaultRealmApi implements RealmApi {
    private final KeycloakSession session;
    private final RealmModel realm;

    public DefaultRealmApi(KeycloakSession session) {
        this.session = session;
        this.realm = Objects.requireNonNull(session.getContext().getRealm());
    }

    @Path("clients")
    @Override
    public ClientsApi clients() {
        return session.getProvider(ClientsApi.class);
    }

    @Override
    public void close() {}
}
