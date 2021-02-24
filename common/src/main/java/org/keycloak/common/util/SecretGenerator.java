package org.keycloak.common.util;

import java.security.SecureRandom;

public class SecretGenerator {

    public static final int DEFAULT_BYTES = 32;

    public static String generate() {
        return generate(DEFAULT_BYTES);
    }

    public static String generate(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return Base62.encodeToString(buf);
    }

}
