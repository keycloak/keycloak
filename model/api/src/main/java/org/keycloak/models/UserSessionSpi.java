package org.keycloak.models;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionSpi implements Spi {

    @Override
    public String getName() {
        return "userSessions";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return UserSessionProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return UserSessionProviderFactory.class;
    }

}
