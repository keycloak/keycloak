package org.keycloak.admin.api.mapper;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class MapStructApiModelMapperFactory implements ApiModelMapperFactory {
    public static final String PROVIDER_ID = "default";
    private static ApiModelMapper SINGLETON;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public ApiModelMapper create(KeycloakSession session) {
        if (SINGLETON == null) {
            SINGLETON = new MapStructApiModelMapper();
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
