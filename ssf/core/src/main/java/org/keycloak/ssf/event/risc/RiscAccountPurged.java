package org.keycloak.ssf.event.risc;

import java.util.Map;

import org.keycloak.ssf.event.InitiatingEntity;
import org.keycloak.ssf.event.InitiatingEntityAware;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Account Purged event indicates that an account has been permanently deleted.
 *
 * <p>Unlike {@link RiscAccountDisabled}, RISC defines no {@code reason} claim for this
 * event — a purge is terminal and carries no rationale enumeration.
 *
 * <p>Receivers use this to drive their own data-retention cleanup, so it is emitted only
 * for a genuine purge. Keycloak has several code paths that delete a user row without the
 * account ceasing to exist — most notably a partial import under the OVERWRITE policy,
 * which deletes and immediately recreates the user — and those deliberately do not emit.
 *
 * <p><b>Known gap.</b> Emission is driven by the admin / user event that follows a
 * deletion, so deletion paths that fire neither produce no event. The significant one is
 * the workflow delete step ({@code DeleteUserStepProvider}): the workflow engine raises no
 * Keycloak events at all, so scheduled data-retention deletions are currently silent —
 * exactly the case receivers would most expect to hear about. Deleting an organization's
 * managed members is silent for the same reason. Both are tracked separately from the
 * initial account-purged support.
 *
 * See: https://openid.net/specs/openid-risc-1_0-final.html
 */
public class RiscAccountPurged extends RiscEvent implements InitiatingEntityAware {

    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/account-purged";

    /**
     * The time of the event (UNIX timestamp). RISC 1.0 lists no attributes for
     * account-purged, so this is a CAEP-defined claim carried as an extension —
     * declared here rather than on {@link RiscEvent} because RISC does not define
     * it profile-wide. Nullable so an absent timestamp is omitted from the wire
     * JSON rather than serialized as {@code "event_timestamp": 0}.
     */
    @JsonProperty("event_timestamp")
    protected Long eventTimestamp;

    /**
     * The entity that initiated the deletion — {@link InitiatingEntity#ADMIN} for a
     * deletion through the admin API, {@link InitiatingEntity#USER} for self-service
     * deletion from the account console. Unlike account-disabled, there is no policy
     * -driven trigger to account for: Keycloak never purges an account on its own.
     */
    @JsonProperty("initiating_entity")
    protected InitiatingEntity initiatingEntity;

    public RiscAccountPurged() {
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
