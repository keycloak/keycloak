package org.keycloak.scim.model.user;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.scim.resource.spi.ScimResourceTypeProviderFactory;

public class UserResourceTypeProviderFactory implements ScimResourceTypeProviderFactory<UserResourceTypeProvider> {

    @Override
    public UserResourceTypeProvider create(KeycloakSession session) {
        return new UserResourceTypeProvider(session);
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
        return "Users";
    }
}
