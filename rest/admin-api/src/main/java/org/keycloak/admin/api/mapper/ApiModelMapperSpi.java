package org.keycloak.admin.api.mapper;

import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ApiModelMapperSpi implements Spi {
    public static final String NAME = "api-model-mapper";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends ApiModelMapper> getProviderClass() {
        return ApiModelMapper.class;
    }

    @Override
    public Class<? extends ProviderFactory<ApiModelMapper>> getProviderFactoryClass() {
        return ApiModelMapperFactory.class;
    }

    @Override
    public boolean isInternal() {
        return true;
    }
}
