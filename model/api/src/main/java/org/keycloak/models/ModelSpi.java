package org.keycloak.models;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ModelSpi implements Spi {

    @Override
    public String getName() {
        return "model";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ModelProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ModelProviderFactory.class;
    }

}
