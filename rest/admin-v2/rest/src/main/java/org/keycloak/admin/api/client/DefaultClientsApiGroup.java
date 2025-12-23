package org.keycloak.admin.api.client;

import jakarta.ws.rs.Path;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.RealmAdminResource;

public class DefaultClientsApiGroup implements ClientsApiGroup {
    private final KeycloakSession session;
    private final RealmAdminResource realmAdminResource;

    public DefaultClientsApiGroup(KeycloakSession session, RealmAdminResource realmAdminResource) {
        this.session = session;
        this.realmAdminResource = realmAdminResource;
    }

    @Path("clients")
    @Override
    public ClientsApi clients() {
        return new DefaultClientsApi(session, realmAdminResource);
    }
}
