package org.keycloak.scim.model.resourcetype;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.scim.resource.spi.ScimResourceTypeProviderFactory;

public class ResourceTypeProviderFactory implements ScimResourceTypeProviderFactory<ResourceTypeProvider> {

    @Override
    public ResourceTypeProvider create(KeycloakSession session) {
        return new ResourceTypeProvider(session);
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

    @Override
    public String getId() {
        return "ResourceTypes";
    }
}
