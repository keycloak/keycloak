package org.keycloak.ssf.event.risc;

import java.util.Map;

import org.keycloak.ssf.event.InitiatingEntity;
import org.keycloak.ssf.event.InitiatingEntityAware;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Account Disabled event indicates that an account has been deactivated, with an
 * optional rationale.
 *
 * See: https://openid.net/specs/openid-risc-1_0-final.html
 */
public class RiscAccountDisabled extends RiscEvent implements InitiatingEntityAware {

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

    /**
     * The time of the event (UNIX timestamp). RISC 1.0 lists no attributes for
     * account-disabled, so this is a CAEP-defined claim carried as an extension —
     * declared here rather than on {@link RiscEvent} because RISC does not define
     * it profile-wide. Nullable so an absent timestamp is omitted from the wire
     * JSON rather than serialized as {@code "event_timestamp": 0}.
     */
    @JsonProperty("event_timestamp")
    protected Long eventTimestamp;

    /**
     * The entity that initiated the disablement. Distinguishes an administrator
     * acting ({@link InitiatingEntity#ADMIN}) from Keycloak's own brute-force
     * policy engine ({@link InitiatingEntity#POLICY}) — a distinction receivers
     * act on, so it is emitted even though RISC does not define the claim.
     */
    @JsonProperty("initiating_entity")
    protected InitiatingEntity initiatingEntity;

    public RiscAccountDisabled() {
        super(TYPE);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(long eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    @Override
    public InitiatingEntity getInitiatingEntity() {
        return initiatingEntity;
    }

    @Override
    public void setInitiatingEntity(InitiatingEntity initiatingEntity) {
        this.initiatingEntity = initiatingEntity;
    }

    @Override
    protected void appendFields(Map<String, Object> fields) {
        super.appendFields(fields);
        fields.put("reason", reason);
        fields.put("eventTimestamp", eventTimestamp);
        fields.put("initiatingEntity", initiatingEntity);
    }
}
