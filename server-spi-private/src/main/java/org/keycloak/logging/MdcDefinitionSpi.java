package org.keycloak.logging;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * This SPI is used to define the MDC keys and values that should be set for each request.
 *
 * @author <a href="mailto:b.eicki@gmx.net">Björn Eickvonder</a>
 */
public class MdcDefinitionSpi implements Spi {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "mdcDefinition";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return MdcDefinitionProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return MdcDefinitionProviderFactory.class;
    }
}
