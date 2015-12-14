package org.keycloak.protocol;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.spi.RealmResourceProviderFactory;

/**
 * Created by fabricio on 11/12/2015.
 */
public class ForgotPasswordProviderFactory implements RealmResourceProviderFactory {

    public ForgotPasswordProvider create(RealmModel realm, KeycloakSession keycloakSession) {
        return new ForgotPasswordProvider(realm, keycloakSession);
    }

    @Override
    public ForgotPasswordProvider create(KeycloakSession session) {
        return new ForgotPasswordProvider(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "forgot-password-email";
    }
}
