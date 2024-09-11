package org.keycloak.services.mapper;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelMapperFactory;
import org.keycloak.models.ModelMapper;
import org.mapstruct.factory.Mappers;

public class DefaultModelMapperFactory implements ModelMapperFactory {
    public static final String PROVIDER_ID = "default";
    private static final ModelMapper SINGLETON = Mappers.getMapper(ModelMapper.class);

    @Override
    public ModelMapper create(KeycloakSession session) {
        return SINGLETON;
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
