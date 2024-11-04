package org.keycloak.federation.scim.jpa;

import org.keycloak.Config.Scope;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class ScimResourceProviderFactory implements JpaEntityProviderFactory {

    static final String ID = "scim-resource";

    @Override
    public JpaEntityProvider create(KeycloakSession session) {
        return new ScimResourceProvider();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(Scope scope) {
        // Nothing to initialise
    }

    @Override
    public void postInit(KeycloakSessionFactory sessionFactory) {
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to close
    }

}
