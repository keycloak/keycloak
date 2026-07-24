package org.keycloak.logging;

import org.keycloak.provider.Spi;

/**
 * This SPI is used to define the MDC keys and values that should be set for each request.
 *
 * @author <a href="mailto:b.eicki@gmx.net">Björn Eickvonder</a>
 */
public class MappedDiagnosticContextSpi implements Spi {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "mappedDiagnosticContext";
    }

    @Override
    public Class<MappedDiagnosticContextProvider> getProviderClass() {
        return MappedDiagnosticContextProvider.class;
    }

    @Override
    public Class<MappedDiagnosticContextProviderFactory> getProviderFactoryClass() {
        return MappedDiagnosticContextProviderFactory.class;
    }
}
