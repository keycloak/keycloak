package org.keycloak.models.mapper;

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
