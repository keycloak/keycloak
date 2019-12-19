package org.keycloak.models.credential.dto;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;

public class PasswordSecretData {

    public static final Logger logger = Logger.getLogger(PasswordSecretData.class);

    private final String value;
    private final byte[] salt;

    @JsonCreator
    public PasswordSecretData(@JsonProperty("value") String value, @JsonProperty("salt") String salt) {
        this(value, decodeSalt(salt));
    }

    public PasswordSecretData(String value, byte[] salt) {
        this.value = value;
        this.salt = salt;
    }

    private static byte[] decodeSalt(String salt) {
        try {
            return Base64.decode(salt);
        } catch (IOException ioe) {
            // Could happen under some corner cases that value is still placeholder value "__SALT__" . For example when importing JSON from
            // previous version and using custom hash provider without salt support.
            logger.tracef("Can't base64 decode the salt %s . Fallback to null salt", salt);
            return null;
        }
    }

    public String getValue() {
        return value;
    }

    public byte[] getSalt() {
        return salt;
    }
}
