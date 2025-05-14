package org.keycloak.models;


import java.io.Serializable;

public class TrustedDevicePolicy implements Serializable {
    public static final boolean DEFAULT_IS_ENABLED = false;
    public static final int DEFAULT_EXPIRATION = 604800; // 7 days = 7 * 24 * 60 * 60

    public static final String REALM_IS_ENABLED_ATTRIBUTE = "realmTrustedDevicesEnabled";
    public static final String REALM_EXPIRATION_ATTRIBUTE = "realmTrustedDevicesExpiration";

    public static final TrustedDevicePolicy DEFAULT_POLICY = new TrustedDevicePolicy(DEFAULT_IS_ENABLED, DEFAULT_EXPIRATION);

    protected boolean isEnabled;
    // Trust expiration in seconds
    protected int trustExpiration;

    public TrustedDevicePolicy() {
    }

    public TrustedDevicePolicy(boolean isEnabled, int trustExpiration) {
        this.isEnabled = isEnabled;
        this.trustExpiration = trustExpiration;
    }


    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public int getTrustExpiration() {
        return trustExpiration;
    }

    public void setTrustExpiration(int trustExpiration) {
        this.trustExpiration = trustExpiration;
    }
}
