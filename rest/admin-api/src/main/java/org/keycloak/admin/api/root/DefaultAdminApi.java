package org.keycloak.admin.api.root;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.keycloak.admin.api.ChosenBySpi;
import org.keycloak.admin.api.realm.RealmsApi;

@RequestScoped
@ChosenBySpi
public class DefaultAdminApi implements AdminApi {

    @Inject
    RealmsApi realmsApi;

    @Path("realms")
    @Override
    public RealmsApi realms() {
        return realmsApi;
    }

    @Override
    public void close() {

    }
}
