package org.keycloak.protocol.ssf.endpoint.admin;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;

public class SsfAdminRealmResourceProviderFactory implements AdminRealmResourceProviderFactory {

    @Override
    public String getId() {
        return "ssf";
    }

    @Override
    public AdminRealmResourceProvider create(KeycloakSession session) {
        return new SsfAdminRealmResourceProvider();
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }
}
