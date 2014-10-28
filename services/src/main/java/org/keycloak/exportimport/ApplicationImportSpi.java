package org.keycloak.exportimport;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ApplicationImportSpi implements Spi {

    @Override
    public String getName() {
        return "application-import";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ApplicationImporter.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ApplicationImporterFactory.class;
    }
}
