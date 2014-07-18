package org.keycloak.picketlink;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PartitionManagerSpi implements Spi {
    @Override
    public String getName() {
        return "picketlink-idm";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return PartitionManagerProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return PartitionManagerProviderFactory.class;
    }
}
