package org.keycloak.credential.hash;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Provider factory for SHA512 variant of the PBKDF2 password hash algorithm.
 *
 * @author @author <a href="mailto:abkaplan07@gmail.com">Adam Kaplan</a>
 */
public class Pbkdf2Sha512PasswordHashProviderFactory implements PasswordHashProviderFactory {

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new Pbkdf2Sha512PasswordHashProvider();
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
        return Pbkdf2Sha512PasswordHashProvider.ALIAS;
    }
}
