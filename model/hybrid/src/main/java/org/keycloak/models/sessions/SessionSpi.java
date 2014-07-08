package org.keycloak.models.sessions;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SessionSpi implements Spi {

    @Override
    public String getName() {
        return "modelSessions";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return SessionProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return SessionProviderFactory.class;
    }

}
