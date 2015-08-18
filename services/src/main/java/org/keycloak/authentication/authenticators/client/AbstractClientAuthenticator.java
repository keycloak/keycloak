package org.keycloak.authentication.authenticators.client;

import org.keycloak.Config;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractClientAuthenticator implements ClientAuthenticator, ClientAuthenticatorFactory {

    @Override
    public ClientAuthenticator create() {
        return this;
    }

    @Override
    public void close() {

    }

    @Override
    public ClientAuthenticator create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }
}
