package org.keycloak.testframework.tests.providers.single;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.AccountResourceProviderFactory;

public class ProviderWithExtraResourcesProviderFactory implements AccountResourceProviderFactory {

    final static String ID = "provider-with-extra-resources";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ProviderWithExtraResourcesProvider create(KeycloakSession session) {
        return new ProviderWithExtraResourcesProvider();
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {

    }
}
