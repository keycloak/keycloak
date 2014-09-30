package org.keycloak.connections.infinispan;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanConnectionSpi implements Spi {

    @Override
    public String getName() {
        return "connectionsInfinispan";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return InfinispanConnectionProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return InfinispanConnectionProviderFactory.class;
    }

}
