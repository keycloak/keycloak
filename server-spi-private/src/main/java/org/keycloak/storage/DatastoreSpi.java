package org.keycloak.storage;

import org.keycloak.provider.Spi;

public class DatastoreSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "datastore";
    }

    @Override
    public Class<DatastoreProvider> getProviderClass() {
        return DatastoreProvider.class;
    }

    @Override
    public Class<DatastoreProviderFactory> getProviderFactoryClass() {
        return DatastoreProviderFactory.class;
    }
    
}
