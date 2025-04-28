package org.keycloak.admin.realm;

import org.keycloak.Config;
import org.keycloak.admin.api.realm.RealmsApi;
import org.keycloak.admin.api.realm.RealmsApiFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultRealmsApiFactory implements RealmsApiFactory {
    public static final String PROVIDER_ID = "default";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public RealmsApi create(KeycloakSession session) {
        return null;
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
