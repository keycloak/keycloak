package org.keycloak.models.cache;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CacheModelProviderSpi implements Spi {

    @Override
    public String getName() {
        return "modelCache";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return CacheModelProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return CacheModelProviderFactory.class;
    }
}
