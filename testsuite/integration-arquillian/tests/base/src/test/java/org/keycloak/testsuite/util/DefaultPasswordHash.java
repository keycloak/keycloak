package org.keycloak.testsuite.util;

import org.keycloak.common.crypto.FipsMode;
import org.keycloak.credential.hash.Pbkdf2Sha512PasswordHashProviderFactory;
import org.keycloak.crypto.hash.Argon2Parameters;
import org.keycloak.crypto.hash.Argon2PasswordHashProviderFactory;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;

public class DefaultPasswordHash {

    public static String getDefaultAlgorithm() {
        return notFips() ? Argon2PasswordHashProviderFactory.ID : Pbkdf2Sha512PasswordHashProviderFactory.ID;
    }

    public static int getDefaultIterations() {
        return notFips() ? Argon2Parameters.DEFAULT_ITERATIONS : Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS;
    }

    private static boolean notFips() {
        return AuthServerTestEnricher.AUTH_SERVER_FIPS_MODE == FipsMode.DISABLED;
    }

}
