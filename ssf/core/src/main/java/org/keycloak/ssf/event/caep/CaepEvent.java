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

    /**
     * The legacy CAEP SSE subject attribute for backwards compatibility. In the OpenID Shared Signals and Events Framework Specification 1.0 - draft 01
     * the subject attribute was used to identify the subject of the event.
     * See: https://openid.net/specs/openid-sse-framework-1_0-ID1.html#rfc.section.3.3
     *
     * In SSF 1.0 the subject is encoded as the "sub_id" claim in the Security Event Token (SET).
     * See: https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-3.1
     */
    @JsonProperty("subject")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    protected SubjectId subjectId;

    /**
     * The time of the event (UNIX timestamp). Nullable so an absent timestamp
     * is omitted from wire JSON rather than serialized as
     * {@code "event_timestamp": 0}.
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
