package org.keycloak.protocol.saml;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 *
 */
public class ArtifactResolverSpi implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "saml-artifact-resolver";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ArtifactResolver.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ArtifactResolverFactory.class;
    }
}
