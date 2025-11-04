package org.keycloak.protocol.ssf.spi;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class DefaultSsfProviderFactory implements SsfProviderFactory, EnvironmentDependentProviderFactory {

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public SsfProvider create(KeycloakSession keycloakSession) {
        return new DefaultSsfProvider(keycloakSession);
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SSF);
    }
}
