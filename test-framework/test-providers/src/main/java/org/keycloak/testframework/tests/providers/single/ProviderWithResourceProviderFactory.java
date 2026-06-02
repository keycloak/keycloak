package org.keycloak.testframework.tests.providers.single;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class ProviderWithResourceProviderFactory implements RealmResourceProviderFactory {

    static final String ID = "provider-with-resource";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ProviderWithResourceProvider create(KeycloakSession session) {
        return new ProviderWithResourceProvider();
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }
}
