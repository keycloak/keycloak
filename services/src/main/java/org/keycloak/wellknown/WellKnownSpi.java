package org.keycloak.wellknown;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class WellKnownSpi implements Spi {

    @Override
    public boolean isPrivate() {
        return true;
    }

    @Override
    public String getName() {
        return "well-known";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return WellKnownProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return WellKnownProviderFactory.class;
    }

}
