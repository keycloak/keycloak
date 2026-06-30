package org.keycloak.representations;

import org.keycloak.common.util.Time;

import com.fasterxml.jackson.annotation.JsonProperty;


public class TrustedDeviceToken extends JsonWebToken {

    @JsonProperty("device_id")
    private String deviceId;

    public TrustedDeviceToken() {
    }

    public TrustedDeviceToken(String id, String secret, Long exp) {
        this.id = id;
        this.deviceId = secret;
        iat((long) Time.currentTime());
        exp(exp);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
