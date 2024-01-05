package org.keycloak.protocol.oid4vc.issuance.signing;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class VCSigningServiceSpi implements Spi {
    private static final String NAME = "vcSigningService";

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
        return VerifiableCredentialsSigningService.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return VCSigningServiceProviderFactory.class;
    }
}
