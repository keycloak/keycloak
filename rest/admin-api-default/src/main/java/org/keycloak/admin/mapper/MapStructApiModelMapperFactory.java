package org.keycloak.admin.mapper;

import org.keycloak.Config;
import org.keycloak.admin.api.mapper.ApiModelMapper;
import org.keycloak.admin.api.mapper.ApiModelMapperFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.mapstruct.factory.Mappers;

public class MapStructApiModelMapperFactory implements ApiModelMapperFactory {
    public static final String PROVIDER_ID = "default";
    private static final ApiModelMapper SINGLETON = Mappers.getMapper(MapStructApiModelMapper.class);

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public ApiModelMapper create(KeycloakSession session) {
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
