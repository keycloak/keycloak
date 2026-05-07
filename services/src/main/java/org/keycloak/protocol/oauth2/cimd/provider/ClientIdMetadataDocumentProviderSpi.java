package org.keycloak.protocol.oauth2.cimd.provider;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * The class is the SPI for {@link ClientIdMetadataDocumentProvider} and {@link ClientIdMetadataDocumentProviderFactory}.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientIdMetadataDocumentProviderSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "cimd";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ClientIdMetadataDocumentProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ClientIdMetadataDocumentProviderFactory.class;
    }
}
