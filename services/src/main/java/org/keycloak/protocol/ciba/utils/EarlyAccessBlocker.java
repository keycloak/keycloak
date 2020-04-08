package org.keycloak.protocol.ciba.utils;

import java.util.HashMap;
import java.util.Map;

public class EarlyAccessBlocker {
    private static final String EXPIRATION_NOTE = "exp";

    private final int expiration;

    public EarlyAccessBlocker(int expiration, int interval) {
        this.expiration = expiration;
    }

    private EarlyAccessBlocker(Map<String, String> data) {
        expiration = Integer.parseInt(data.get(EXPIRATION_NOTE));
    }

    public static final EarlyAccessBlocker deserializeCode(Map<String, String> data) {
        return new EarlyAccessBlocker(data);
    }

    public Map<String, String> serializeCode() {
        Map<String, String> result = new HashMap<>();
        result.put(EXPIRATION_NOTE, String.valueOf(expiration));
        return result;
    }

    public int getExpiration() {
        return expiration;
    }


}
