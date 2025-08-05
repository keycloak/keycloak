package org.keycloak.admin.api.realm;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.admin.api.client.DefaultClientsApi;

@RequestScoped
public class DefaultRealmApi implements RealmApi {

    @Inject
    DefaultClientsApi clientsApi;

    @Path("clients")
    @Override
    public ClientsApi clients() {
        return clientsApi;
    }
}
