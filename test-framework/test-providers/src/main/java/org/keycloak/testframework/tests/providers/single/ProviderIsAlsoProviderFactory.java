package org.keycloak.testframework.tests.providers.single;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class ProviderIsAlsoProviderFactory implements RealmResourceProviderFactory, RealmResourceProvider {

    final static String ID =  "provider-is-also-provider-factory";

    @Override
    public ProviderIsAlsoProviderFactory create(KeycloakSession session) {
        return this;
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
    public ProviderIsAlsoProviderFactory getResource() {
        return this;
    }
}
