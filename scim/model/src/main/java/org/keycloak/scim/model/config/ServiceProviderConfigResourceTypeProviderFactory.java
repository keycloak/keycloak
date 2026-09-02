package org.keycloak.scim.model.config;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.scim.resource.spi.ScimResourceTypeProviderFactory;

public class ServiceProviderConfigResourceTypeProviderFactory implements ScimResourceTypeProviderFactory<ServiceProviderConfigResourceTypeProvider> {

    @Override
    public ServiceProviderConfigResourceTypeProvider create(KeycloakSession session) {
        return new ServiceProviderConfigResourceTypeProvider(session);
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
        return "ServiceProviderConfig";
    }
}
