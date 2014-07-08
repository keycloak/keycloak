package org.keycloak.exportimport;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ImportSpi implements Spi {

    @Override
    public String getName() {
        return "import";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ImportProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ImportProviderFactory.class;
    }
}
