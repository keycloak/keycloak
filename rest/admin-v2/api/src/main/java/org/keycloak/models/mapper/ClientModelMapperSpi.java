package org.keycloak.models.mapper;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ClientModelMapperSpi implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "client-model-mapper";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ClientModelMapper.class;
    }

    @Override
    public Class<? extends ProviderFactory<ClientModelMapper>> getProviderFactoryClass() {
        return ClientModelMapperFactory.class;
    }
}
