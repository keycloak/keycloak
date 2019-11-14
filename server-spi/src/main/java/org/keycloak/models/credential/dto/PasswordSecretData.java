package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordSecretData {
    private final String value;
    private final byte[] salt;

    @JsonCreator
    public PasswordSecretData(@JsonProperty("value") String value, @JsonProperty("salt") byte[] salt) {
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
