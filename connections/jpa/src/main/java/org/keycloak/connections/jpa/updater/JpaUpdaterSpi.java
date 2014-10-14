package org.keycloak.connections.jpa.updater;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaUpdaterSpi implements Spi {

    @Override
    public String getName() {
        return "connectionsJpaUpdater";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return JpaUpdaterProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return JpaUpdaterProviderFactory.class;
    }

}
