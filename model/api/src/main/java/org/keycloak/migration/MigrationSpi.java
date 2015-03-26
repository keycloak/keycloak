package org.keycloak.migration;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrationSpi implements Spi {

    @Override
    public String getName() {
        return "migration";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return MigrationProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return MigrationProviderFactory.class;
    }
}
