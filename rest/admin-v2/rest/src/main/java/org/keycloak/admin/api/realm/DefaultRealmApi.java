package org.keycloak.admin.api.realm;

import jakarta.ws.rs.Path;

import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.admin.api.client.DefaultClientsApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.RealmAdminResource;

public class DefaultRealmApi implements RealmApi {
    private final KeycloakSession session;
    private final RealmAdminResource realmAdminResource;

    public DefaultRealmApi(KeycloakSession session, RealmAdminResource realmAdmin) {
        this.session = session;
        this.realmAdminResource = realmAdmin;
    }

    @Path("clients")
    @Override
    public ClientsApi clients() {
        return new DefaultClientsApi(session, realmAdminResource.getClients());
    }

}
