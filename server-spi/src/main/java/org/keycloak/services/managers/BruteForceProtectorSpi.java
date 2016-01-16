package org.keycloak.services.managers;

import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class BruteForceProtectorSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "bruteForceProtector";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return BruteForceProtector.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return BruteForceProtectorFactory.class;
    }

}
