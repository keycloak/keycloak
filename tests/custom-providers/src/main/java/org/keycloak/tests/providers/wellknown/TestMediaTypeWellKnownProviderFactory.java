package org.keycloak.tests.providers.wellknown;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

public class TestMediaTypeWellKnownProviderFactory implements WellKnownProviderFactory {

    public static final String ID = "test-media-type";

    @Override
    public WellKnownProvider create(KeycloakSession session) {
        return new TestMediaTypeWellKnownProvider();
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
        return ID;
    }

    @Override
    public boolean isAvailableViaServerMetadata() {
        return true;
    }
}
