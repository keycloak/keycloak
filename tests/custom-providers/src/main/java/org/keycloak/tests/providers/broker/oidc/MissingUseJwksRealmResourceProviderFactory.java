package org.keycloak.tests.providers.broker.oidc;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class MissingUseJwksRealmResourceProviderFactory implements RealmResourceProviderFactory {

    public static final String ID = "missing-use-jwks";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new MissingUseJwksRealmResourceProvider(session);
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
