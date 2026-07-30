package org.keycloak.ssf.event.risc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Account Disabled event indicates that an account has been deactivated, with an
 * optional rationale.
 *
 * See: https://openid.net/specs/openid-risc-1_0-final.html
 */
public class RiscAccountDisabled extends RiscEvent {

    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/account-disabled";

    /**
     * RISC enumerates {@code hijacking} and {@code bulk-account} as reasons. Keycloak
     * additionally emits reasons describing its own native disablement triggers
     * (administrator action, brute-force lockout, ...) that aren't part of the spec's
     * list — mirroring how {@link org.keycloak.ssf.event.caep.CaepCredentialChange#getCredentialType()}
     * accepts values beyond its documented set. Receivers that don't recognize a given
     * reason should treat it as an opaque string.
     */
    public static final String REASON_HIJACKING = "hijacking";
    public static final String REASON_BULK_ACCOUNT = "bulk-account";
    public static final String REASON_ADMIN = "disabled-by-admin";
    public static final String REASON_BRUTE_FORCE = "disabled-by-bruteforce";

    @JsonProperty("reason")
    protected String reason;

    public RiscAccountDisabled() {
        super(TYPE);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "AccountDisabled{" +
               "reason='" + reason + '\'' +
               '}';
    }
}
