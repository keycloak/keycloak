package org.keycloak.testsuite.oidc;

import org.keycloak.common.util.Base64Url;

import java.security.MessageDigest;
import java.util.UUID;

public class PkceGenerator {

    private String codeVerifier;

    private String codeChallenge;

    public PkceGenerator() {
        codeVerifier = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString(); // Good enough for testing, but shouldn't be used elsewhere
        codeChallenge = generateS256CodeChallenge(codeVerifier);
    }

    public PkceGenerator(String codeVerifier) {
        this.codeVerifier = codeVerifier;
        codeChallenge = generateS256CodeChallenge(codeVerifier);
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    private String generateS256CodeChallenge(String codeVerifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(codeVerifier.getBytes("ISO_8859_1"));
            byte[] digestBytes = md.digest();
            String codeChallenge = Base64Url.encode(digestBytes);
            return codeChallenge;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
