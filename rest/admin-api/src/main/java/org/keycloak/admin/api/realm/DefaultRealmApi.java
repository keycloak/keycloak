package org.keycloak.admin.api.realm;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.keycloak.admin.api.ChosenBySpi;
import org.keycloak.admin.api.client.ClientsApi;

@RequestScoped
@ChosenBySpi
public class DefaultRealmApi implements RealmApi {

    @Inject
    ClientsApi clientsApi;

    @Path("clients")
    @Override
    public ClientsApi clients() {
        return clientsApi;
    }

    @Override
    public void close() {

    }
}
