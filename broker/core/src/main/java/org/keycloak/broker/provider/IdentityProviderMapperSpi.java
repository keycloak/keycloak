package org.keycloak.broker.provider;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityProviderMapperSpi implements Spi {

    @Override
    public String getName() {
        return "identity-provider-mapper";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return IdentityProviderMapper.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return IdentityProviderMapper.class;
    }

}
