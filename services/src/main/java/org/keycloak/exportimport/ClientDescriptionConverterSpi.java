package org.keycloak.exportimport;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientDescriptionConverterSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "client-description-converter";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ClientDescriptionConverter.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ClientDescriptionConverterFactory.class;
    }

}
