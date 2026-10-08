package org.keycloak.services.resources.admin.ext.untrustedlogin;

import org.keycloak.models.RealmModel;

import org.keycloak.models.untrustedlogin.UntrustedLoginRealmConfig;

/**
 * Wire representation for GET/PUT /admin/realms/{realm}/untrusted-login-notifications/config
 */
public class UntrustedLoginConfigRepresentation {

    private boolean enabled;
    private int trustWindowDays;

    public UntrustedLoginConfigRepresentation() {
        // required for JSON deserialization
    }

    public UntrustedLoginConfigRepresentation(boolean enabled, int trustWindowDays) {
        this.enabled = enabled;
        this.trustWindowDays = trustWindowDays;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTrustWindowDays() {
        return trustWindowDays;
    }

    public void setTrustWindowDays(int trustWindowDays) {
        this.trustWindowDays = trustWindowDays;
    }

    public static UntrustedLoginConfigRepresentation fromRealm(RealmModel realm) {
        return new UntrustedLoginConfigRepresentation(
                UntrustedLoginRealmConfig.isEnabled(realm),
                UntrustedLoginRealmConfig.getTrustWindowDays(realm));
    }
}