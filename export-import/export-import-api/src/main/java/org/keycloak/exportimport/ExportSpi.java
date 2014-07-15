package org.keycloak.exportimport;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportSpi implements Spi {

    @Override
    public String getName() {
        return "export";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ExportProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ExportProviderFactory.class;
    }
}
