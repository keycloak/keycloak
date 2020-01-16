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
    public PasswordSecretData(@JsonProperty("value") String value, @JsonProperty("salt") String salt) throws IOException {
        if (salt == null || "__SALT__".equals(salt)) {
            this.value = value;
            this.salt = null;
        }
        else {
            this.value = value;
            this.salt = Base64.decode(salt);
        }
    }

    public PasswordSecretData(String value, byte[] salt) {
        this.value = value;
        this.salt = salt;
    }

    public String getValue() {
        return value;
    }

    public byte[] getSalt() {
        return salt;
    }
}
