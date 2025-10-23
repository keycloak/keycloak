package org.keycloak.models.mapper;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class MapStructModelMapperFactory implements ModelMapperFactory {
    public static final String PROVIDER_ID = "default";
    private static ModelMapper SINGLETON;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public ModelMapper create(KeycloakSession session) {
        if (SINGLETON == null) {
            SINGLETON = new MapStructModelMapper();
        }
        return SINGLETON;
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
