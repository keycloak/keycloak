package org.keycloak.models.mapper;

import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ModelMapperSpi implements Spi {
    public static final String NAME = "model-mapper";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends ModelMapper> getProviderClass() {
        return ModelMapper.class;
    }

    @Override
    public Class<? extends ProviderFactory<ModelMapper>> getProviderFactoryClass() {
        return ModelMapperFactory.class;
    }

    @Override
    public boolean isInternal() {
        return true;
    }
}
