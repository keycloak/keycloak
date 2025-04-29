package org.keycloak.admin.api.mapper;

import org.mapstruct.factory.Mappers;

public class MapStructApiModelMapper implements ApiModelMapper {
    private final MapStructClientMapper clientMapper;

    public MapStructApiModelMapper() {
        this.clientMapper = Mappers.getMapper(MapStructClientMapper.class);
    }

    @Override
    public ApiClientMapper clients() {
        return clientMapper;
    }
}
