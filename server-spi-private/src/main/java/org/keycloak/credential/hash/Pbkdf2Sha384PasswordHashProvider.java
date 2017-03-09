package org.keycloak.credential.hash;

/**
 * PBKDF2 password hash provider with HMAC using SHA384
 *
 * @author <a href"mailto:abkaplan07@gmail.com">Adam Kaplan</a>
 */
public class Pbkdf2Sha384PasswordHashProvider extends APbkdf2PasswordHashProvider {

    static final String ALIAS = "pbkdf2-hmac-sha384";
    @Override
    protected String getAlgorithmAlias() {
        return ALIAS;
    }

    @Override
    protected String getPbkdf2Algorithm() {
        return "PBKDF2WithHmacSHA384";
    }

    @Override
    protected int getDerivedKeySize() {
        // Note: As of Keycloak 2.2 large hash values (4000 characters) are supported.
        return 1024;
    }
}
