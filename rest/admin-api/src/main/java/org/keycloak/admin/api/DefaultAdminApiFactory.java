package org.keycloak.admin.api;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultAdminApiFactory implements AdminApiFactory {
    public static final String PROVIDER_ID = "default";

    @Override
    public Class<DefaultAdminApi> getProviderClass() {
        return DefaultAdminApi.class;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
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
