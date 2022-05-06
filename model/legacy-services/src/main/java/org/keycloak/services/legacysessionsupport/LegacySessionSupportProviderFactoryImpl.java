package org.keycloak.services.legacysessionsupport;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.LegacySessionSupportProvider;

/**
 * @author Alexander Schwartz
 */
public class LegacySessionSupportProviderFactoryImpl implements LegacySessionSupportProviderFactory<LegacySessionSupportProvider> {

    private static final String PROVIDER_ID = "default";

    @Override
    public LegacySessionSupportProvider create(KeycloakSession session) {
        return new LegacySessionSupportProviderImpl(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
