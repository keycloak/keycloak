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

    public static final String ID = "pbkdf2-sha512";

    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA512";

    public static final int DEFAULT_ITERATIONS = 30000;

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new Pbkdf2PasswordHashProvider(ID, PBKDF2_ALGORITHM, DEFAULT_ITERATIONS);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void close() {
    }
}
