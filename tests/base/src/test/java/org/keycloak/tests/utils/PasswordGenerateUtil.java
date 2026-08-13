package org.keycloak.tests.utils;

import org.keycloak.common.util.SecretGenerator;

public class PasswordGenerateUtil {

    public static String generatePassword() {
        return generatePassword(64);
    }

    public static String generatePassword(int length) {
        return SecretGenerator.getInstance().randomString(length);
    }
}
