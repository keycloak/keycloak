package org.keycloak.ssf.event;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectIdJsonDeserializer;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents a generic SSF event.
 *
 * See: https://datatracker.ietf.org/doc/html/rfc8417
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class SsfEvent {

    /**
     * Internal (shorter) alias for the event type.
     */
    @JsonIgnore
    protected String alias;

    @JsonProperty("subject")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    protected SubjectId subjectId;

    @JsonIgnore
    protected String eventType;

    /**
     * The time of the event (UNIX timestamp). Nullable so events that do
     * not carry a timestamp — e.g. {@code ssf/event-type/verification}
     * (SSF §8.1.4 carries only {@code state}) and other stream-management
     * events — are omitted from the wire JSON instead of being serialized
     * as {@code "event_timestamp": 0} (the default value of a primitive
     * {@code long}, which Jackson always emits).
     */
    @JsonProperty("event_timestamp")
    protected Long eventTimestamp;

    /**
     * The entity that initiated the event
     */
    @JsonProperty("initiating_entity")
    protected InitiatingEntity initiatingEntity;

    /**
     * A localized administrative message intended for logging and auditing.
     * key is language code, value is message.
     */
    @JsonProperty("reason_admin")
    protected Map<String, String> reasonAdmin;

    /**
     * A localized message intended for the end user.
     * key is language code, value is message.
     */
    @JsonProperty("reason_user")
    protected Map<String, String> reasonUser;

    @JsonIgnore
    protected Map<String, Object> attributes = new HashMap<>();

    public SsfEvent(String eventType) {
        this.eventType = eventType;

        // use the simple class name as the default alias
        this.alias = getClass().getSimpleName();
    }

    public SubjectId getSubjectId() {
        return subjectId;
    }

    public Long getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(long eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public InitiatingEntity getInitiatingEntity() {
        return initiatingEntity;
    }

    public void setInitiatingEntity(InitiatingEntity initiatingEntity) {
        this.initiatingEntity = initiatingEntity;
    }

    public Map<String, String> getReasonAdmin() {
        return reasonAdmin;
    }

    public void setReasonAdmin(Map<String, String> reasonAdmin) {
        this.reasonAdmin = reasonAdmin;
    }

    public Map<String, String> getReasonUser() {
        return reasonUser;
    }

    public void setReasonUser(Map<String, String> reasonUser) {
        this.reasonUser = reasonUser;
    }

    public String getEventType() {
        return eventType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @JsonAnySetter
    public void setAttributeValue(String key, Object value) {
        attributes.put(key, value);
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setSubjectId(SubjectId subjectId) {
        this.subjectId = subjectId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Verify that this event instance carries the fields the SSF /
     * CAEP / RISC spec marks as REQUIRED. Called by the synthetic-emit
     * pipeline after Jackson has materialised the event from the
     * caller-supplied JSON, so a missing-field mistake is rejected
     * with a clear error before the SET is signed and dispatched.
     *
     * <p>Default implementation is a no-op — most CAEP/RISC events have
     * no strictly-required fields beyond the subject, which the
     * pipeline validates separately. Subclasses with mandatory fields
     * (e.g. {@code CaepCredentialChange.change_type}) override this
     * to throw {@link SsfEventValidationException} with a message
     * naming the missing field.
     *
     * <p>Native event production (the SSF event listener) builds
     * instances from typed Keycloak event details that always supply
     * the required fields, so the hook only matters on the
     * synthetic-emit path. Custom extension events use this same hook
     * to enforce their own invariants.
     */
    public void validate() {
        // no-op — overridden by event subclasses that have spec-required fields
    }
}
