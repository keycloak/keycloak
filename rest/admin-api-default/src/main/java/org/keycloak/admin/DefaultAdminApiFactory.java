package org.keycloak.admin;

import org.keycloak.Config;
import org.keycloak.admin.api.AdminApi;
import org.keycloak.admin.api.AdminApiFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultAdminApiFactory implements AdminApiFactory {
    public static final String PROVIDER_ID = "default";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public AdminApi create(KeycloakSession session) {
        return new DefaultAdminApi(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }
}
