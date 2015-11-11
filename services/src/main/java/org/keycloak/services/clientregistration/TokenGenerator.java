package org.keycloak.services.clientregistration;

import org.keycloak.common.util.Base64Url;

import java.security.SecureRandom;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TokenGenerator {

    private static final int REGISTRATION_ACCESS_TOKEN_BYTES = 32;

    private TokenGenerator() {
    }

    public String createInitialAccessToken() {
        return null;
    }

    public static String createRegistrationAccessToken() {
        byte[] buf = new byte[REGISTRATION_ACCESS_TOKEN_BYTES];
        new SecureRandom().nextBytes(buf);
        return Base64Url.encode(buf);
    }

}
