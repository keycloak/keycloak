package org.keycloak.tests.providers.rest;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class CookieTestingRealmResourceProviderFactory implements RealmResourceProviderFactory {

    static final String ID = "cookie-testing";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new CookieTestingRealmResourceProvider(session);
    }

    @Override
    public String getId() {
        return ID;
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
}
