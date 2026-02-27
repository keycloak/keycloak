package org.keycloak.scim.model.config;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.scim.resource.spi.ScimResourceTypeProviderFactory;

public class ServiceProviderConfigResourceTypeProviderFactory implements ScimResourceTypeProviderFactory<ServiceProviderConfigResourceTypeProvider> {

    public static final ServiceProviderConfigResourceTypeProvider INSTANCE = new ServiceProviderConfigResourceTypeProvider();

    @Override
    public ServiceProviderConfigResourceTypeProvider create(KeycloakSession session) {
        return INSTANCE;
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
