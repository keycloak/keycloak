package org.keycloak.scim.model.schema;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.scim.resource.spi.ScimResourceTypeProviderFactory;

public class SchemaResourceTypeProviderFactory implements ScimResourceTypeProviderFactory<SchemaResourceTypeProvider> {

    public static final String ID = "Schemas";

    @Override
    public SchemaResourceTypeProvider create(KeycloakSession session) {
        return new SchemaResourceTypeProvider(session);
    }

    @Override
    public void init(Scope config) {
        // No initialization needed
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization needed
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public String getId() {
        return ID;
    }
}
