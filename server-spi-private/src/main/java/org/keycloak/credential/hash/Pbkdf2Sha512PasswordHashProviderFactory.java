package org.keycloak.credential.hash;

import org.keycloak.models.KeycloakSession;

/**
 * Provider factory for SHA512 variant of the PBKDF2 password hash algorithm.
 *
 * @author @author <a href="mailto:abkaplan07@gmail.com">Adam Kaplan</a>
 */
public class Pbkdf2Sha512PasswordHashProviderFactory extends AbstractPbkdf2PasswordHashProviderFactory implements PasswordHashProviderFactory {

    public static final String ID = "pbkdf2-sha512";

    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA512";

    /**
     * Hash iterations for PBKDF2-HMAC-SHA512 according to the <a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2">Password Storage Cheat Sheet</a>.
     */
    public static final int DEFAULT_ITERATIONS = 210_000;

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new Pbkdf2PasswordHashProvider(ID, PBKDF2_ALGORITHM, DEFAULT_ITERATIONS, getMaxPaddingLength());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int order() {
        return 200;
    }
}
