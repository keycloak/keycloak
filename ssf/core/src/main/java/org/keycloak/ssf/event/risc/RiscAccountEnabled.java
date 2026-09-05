package org.keycloak.ssf.event.risc;

import java.util.Map;

import org.keycloak.ssf.event.InitiatingEntity;
import org.keycloak.ssf.event.InitiatingEntityAware;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Account Enabled event signals that a previously disabled account has been
 * reactivated.
 *
 * See: https://openid.net/specs/openid-risc-1_0-final.html
 */
public class RiscAccountEnabled extends RiscEvent implements InitiatingEntityAware {

    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/account-enabled";

    /**
     * The time of the event (UNIX timestamp). RISC 1.0 lists no attributes for
     * account-enabled, so this is a CAEP-defined claim carried as an extension —
     * declared here rather than on {@link RiscEvent} because RISC does not define
     * it profile-wide. Nullable so an absent timestamp is omitted from the wire
     * JSON rather than serialized as {@code "event_timestamp": 0}.
     */
    @JsonProperty("event_timestamp")
    protected Long eventTimestamp;

    /**
     * The entity that initiated the re-activation. Only the admin path emits this
     * event today ({@link InitiatingEntity#ADMIN}); a future non-admin trigger
     * would be an automated action rather than the user's own, hence
     * {@link InitiatingEntity#SYSTEM} as its default.
     */
    @JsonProperty("initiating_entity")
    protected InitiatingEntity initiatingEntity;

    public RiscAccountEnabled() {
        super(TYPE);
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
        fields.put("eventTimestamp", eventTimestamp);
        fields.put("initiatingEntity", initiatingEntity);
    }
}
