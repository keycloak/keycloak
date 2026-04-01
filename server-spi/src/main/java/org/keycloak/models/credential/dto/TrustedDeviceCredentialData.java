package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TrustedDeviceCredentialData {
    private final Long expireTime;

    @JsonCreator
    public TrustedDeviceCredentialData(@JsonProperty("expireTime") Long expireTime) {
        this.expireTime = expireTime;
    }

    public Long getExpireTime() {
        return expireTime;
    }
}
