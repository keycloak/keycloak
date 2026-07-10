package org.keycloak.ssf.event.caep;

import java.util.Map;

import org.keycloak.ssf.event.InitiatingEntity;
import org.keycloak.ssf.event.SsfEvent;
import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectIdJsonDeserializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Generic CaepEvent.
 *
 * See: https://openid.net/specs/openid-caep-1_0-final.html
 */
public abstract class CaepEvent extends SsfEvent {

    /**
     * See: https://openid.net/specs/openid-caep-1_0-final.html#section-3
     */
    public static final String EVENT_TYPE_BASE_URI = "https://schemas.openid.net/secevent/caep/event-type/";

    @JsonProperty("subject")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    protected SubjectId subjectId;

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

    public CaepEvent(String type) {
        super(type);
    }

    public SubjectId getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(SubjectId subjectId) {
        this.subjectId = subjectId;
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

    @Override
    protected void appendFields(Map<String, Object> fields) {
        super.appendFields(fields);
        fields.put("subjectId", subjectId);
        fields.put("eventTimestamp", eventTimestamp);
        fields.put("initiatingEntity", initiatingEntity);
        fields.put("reasonAdmin", reasonAdmin);
        fields.put("reasonUser", reasonUser);
    }
}
