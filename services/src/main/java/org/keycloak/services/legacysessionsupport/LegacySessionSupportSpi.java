package org.keycloak.services.legacysessionsupport;

import org.keycloak.models.LegacySessionSupportProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.Spi;

/**
 * @author Alexander Schwartz
 */
public class LegacySessionSupportSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "legacy-session-support";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return LegacySessionSupportProvider.class;
    }

    @Override
    public Class<? extends LegacySessionSupportProviderFactory> getProviderFactoryClass() {
        return LegacySessionSupportProviderFactory.class;
    }

}
