package org.keycloak.picketlink;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityManagerSpi implements Spi {
    @Override
    public String getName() {
        return "picketlink-identity-manager";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return IdentityManagerProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return IdentityManagerProviderFactory.class;
    }
}
