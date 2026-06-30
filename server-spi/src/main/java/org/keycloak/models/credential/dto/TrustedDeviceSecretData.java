package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TrustedDeviceSecretData {
    private final String deviceId;

    @JsonCreator
    public TrustedDeviceSecretData(@JsonProperty("deviceId") String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
