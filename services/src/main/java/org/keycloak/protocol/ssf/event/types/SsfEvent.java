package org.keycloak.protocol.ssf.event.types;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.protocol.ssf.event.subjects.SubjectId;
import org.keycloak.protocol.ssf.event.subjects.SubjectIdJsonDeserializer;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents a generic SSF event.
 *
 * See: https://datatracker.ietf.org/doc/html/rfc8417
 */
public abstract class SsfEvent {

    @JsonProperty("subject")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    protected SubjectId subjectId;

    @JsonIgnore
    protected String eventType;

    /**
     * The time of the event (UNIX timestamp)
     */
    @JsonProperty("event_timestamp")
    protected long eventTimestamp;

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
    }

    public SubjectId getSubjectId() {
        return subjectId;
    }

    public long getEventTimestamp() {
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
}
