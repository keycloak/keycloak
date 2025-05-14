package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TrustedDeviceSecretData {
    private final String value;

    /**
     * Creator for trusted device secret
     * @param value secret value
     */
    @JsonCreator
    public TrustedDeviceSecretData(@JsonProperty("value") String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
