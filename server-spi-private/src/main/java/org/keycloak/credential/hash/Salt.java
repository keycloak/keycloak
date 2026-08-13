package org.keycloak.credential.hash;

import java.security.SecureRandom;

public class Salt {

    public static byte[] generateSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return salt;
    }

}
