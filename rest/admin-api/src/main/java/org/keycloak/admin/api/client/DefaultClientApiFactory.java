package org.keycloak.admin.api.client;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultClientApiFactory implements ClientApiFactory {
    public static final String PROVIDER_ID = "default";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    public Class<? extends ClientApi> getProviderClass() {
        return DefaultClientApi.class;
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
