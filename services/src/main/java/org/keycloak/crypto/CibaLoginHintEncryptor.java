package org.keycloak.crypto;

import org.bouncycastle.util.encoders.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class CibaLoginHintEncryptor {

    private CibaLoginHintEncryptor() {
    }

    public static String encodeLoginHint(String clientSecret, String login) throws GeneralSecurityException {
        byte[] encrypted = Aes128GcmEncryptor.encrypt(clientSecret, login.getBytes(StandardCharsets.UTF_8));
        return Base64.toBase64String(encrypted);
    }

    public static String decodeLoginHint(String clientSecret, String loginHint) throws GeneralSecurityException {
        byte[] decoded = Base64.decode(loginHint);
        return new String(Aes128GcmEncryptor.decrypt(clientSecret,decoded));
    }
}
