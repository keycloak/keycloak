package org.keycloak.credential.hash;

/**
 * PBKDF2 Password Hash provider with HMAC using SHA224
 *
 * @author <a href="mailto:abkaplan07@gmail.com">Adam Kaplan</a>
 */
public class Pbkdf2Sha224PasswordHashProvider extends APbkdf2PasswordHashProvider {

    static String ALIAS = "pbkdf2-hmac-sha224";

    @Override
    protected String getAlgorithmAlias() {
        return ALIAS;
    }

    @Override
    protected String getPbkdf2Algorithm() {
        return "PBKDF2WithHmacSHA224";
    }

    @Override
    protected int getDerivedKeySize() {
        return 224;
    }
}
