package org.keycloak.models.cache;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CacheUserProviderSpi implements Spi {

    @Override
    public String getName() {
        return "userCache";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return CacheUserProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return CacheUserProviderFactory.class;
    }
}
