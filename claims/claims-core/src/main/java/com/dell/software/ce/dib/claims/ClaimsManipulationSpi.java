package com.dell.software.ce.dib.claims;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ClaimsManipulationSpi implements Spi {

    public static final String CLAIMS_MANIPULATION_SPI_NAME = "claims_manipulation";

    @Override
    public String getName() {
        return CLAIMS_MANIPULATION_SPI_NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ClaimsManipulation.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ClaimsManipulationFactory.class;
    }
}
