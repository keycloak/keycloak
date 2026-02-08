package org.keycloak.scim.services;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class ScimRealmResourceFactory implements RealmResourceProviderFactory {

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new RealmResourceProvider() {
            @Override
            public Object getResource() {
                return new ScimRealmResource(session);
            }

            @Override
            public void close() {

            }
        };
    }

    @Override
    public void init(Scope config) {
        config.toString();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.toString();
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "scim";
    }
}
