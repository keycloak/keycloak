package org.keycloak.connections.mongo.updater;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MongoUpdaterSpi implements Spi {

    @Override
    public String getName() {
        return "connectionsMongoUpdater";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return MongoUpdaterProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return MongoUpdaterProviderFactory.class;
    }

}
