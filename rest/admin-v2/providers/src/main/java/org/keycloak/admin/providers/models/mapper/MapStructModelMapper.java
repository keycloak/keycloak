package org.keycloak.admin.providers.models.mapper;

import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.mapper.ModelMapper;

import org.mapstruct.factory.Mappers;

public class MapStructModelMapper implements ModelMapper {
    private final MapStructClientModelMapper clientMapper;

    public MapStructModelMapper() {
        this.clientMapper = Mappers.getMapper(MapStructClientModelMapper.class);
    }

    @Override
    public ClientModelMapper clients() {
        return clientMapper;
    }
}
