package org.keycloak.admin.api.realm;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultRealmApiFactory implements RealmApiFactory {
    public static final String PROVIDER_ID = "default";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Class<? extends RealmApi> getProviderClass() {
        return DefaultRealmApi.class;
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
