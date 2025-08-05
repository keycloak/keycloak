package org.keycloak.admin.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.keycloak.admin.api.realm.DefaultRealmsApi;
import org.keycloak.admin.api.realm.RealmsApi;

@RequestScoped
public class DefaultAdminApi implements AdminApi {

    @Inject
    DefaultRealmsApi realmsApi;

    @Path("realms")
    @Override
    public RealmsApi realms() {
        return realmsApi;
    }

    @Override
    public void close() {

    }
}
