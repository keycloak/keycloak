package org.keycloak.protocol.ciba.utils;

import java.util.HashMap;
import java.util.Map;

public class DecoupledAuthnResult {
    private static final String EXPIRATION_NOTE = "exp";
    private static final String STATUS_NOTE = "status";

    private final int expiration;
    private final String status;

    public DecoupledAuthnResult(int expiration, String status) {
        this.expiration = expiration;
        this.status = status;
    }

    private DecoupledAuthnResult(Map<String, String> data) {
        expiration = Integer.parseInt(data.get(EXPIRATION_NOTE));
        status = data.get(STATUS_NOTE);
    }

    public static final DecoupledAuthnResult deserializeCode(Map<String, String> data) {
        return new DecoupledAuthnResult(data);
    }

    public Map<String, String> serializeCode() {
        Map<String, String> result = new HashMap<>();
        result.put(EXPIRATION_NOTE, String.valueOf(expiration));
        result.put(STATUS_NOTE, status);
        return result;
    }

    public int getExpiration() {
        return expiration;
    }

    public String getStatus() {
        return status;
    }
}
