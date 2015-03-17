package org.keycloak.connections.file;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class FileConnectionSpi implements Spi {

    @Override
    public String getName() {
        return "connectionsFile";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return FileConnectionProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return FileConnectionProviderFactory.class;
    }

}
