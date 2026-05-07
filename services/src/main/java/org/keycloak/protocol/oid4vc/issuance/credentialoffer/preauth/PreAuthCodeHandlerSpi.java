package org.keycloak.protocol.oid4vc.issuance.credentialoffer.preauth;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * SPI for handling the production and verification of pre-authorized codes.
 */
public class PreAuthCodeHandlerSpi implements Spi {

    private static final String NAME = "preAuthCodeHandler";

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return PreAuthCodeHandler.class;
    }

    @Override
    public Class<? extends ProviderFactory<PreAuthCodeHandler>> getProviderFactoryClass() {
        return PreAuthCodeHandlerFactory.class;
    }
}
