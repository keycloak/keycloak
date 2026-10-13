package org.keycloak.testframework.remote.providers.timeoffset;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class TimeOffSetRealmResourceProviderFactory implements RealmResourceProviderFactory {

    private final String ID = "testing-timeoffset";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new TimeOffSetRealmResourceProvider(session);
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
    public void init(org.keycloak.Config.Scope config) {

    }
}
