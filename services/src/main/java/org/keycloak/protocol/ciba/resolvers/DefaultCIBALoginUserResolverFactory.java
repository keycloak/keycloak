package org.keycloak.protocol.ciba.resolvers;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultCIBALoginUserResolverFactory implements CIBALoginUserResolverFactory {

    public static final String PROVIDER_ID = "default-ciba-login-user-resolver";

    @Override
    public CIBALoginUserResolver create(KeycloakSession session) {
        return new DefaultCIBALoginUserResolver(session);
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
        return PROVIDER_ID;
    }

}
