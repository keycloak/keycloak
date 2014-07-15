package org.keycloak.models;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSpi implements Spi {

    @Override
    public String getName() {
        return "user";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return UserProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return UserProviderFactory.class;
    }

}
