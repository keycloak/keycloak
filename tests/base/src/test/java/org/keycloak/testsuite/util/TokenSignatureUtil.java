package org.keycloak.testsuite.util;

import java.util.Map;

public final class TokenSignatureUtil {

    private static final Map<String, String> EC_ALG_TO_NIST = Map.of(
            "ES256", "P-256",
            "ES384", "P-384",
            "ES512", "P-521");

    private TokenSignatureUtil() {
    }

    public static String convertAlgorithmToECDomainParamNistRep(String algorithm) {
        String value = EC_ALG_TO_NIST.get(algorithm);
        if (value == null) {
            throw new RuntimeException("Unknown algorithm: " + algorithm);
        }
        return value;
    }
}
