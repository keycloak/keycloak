package org.keycloak.ssf.transmitter.admin;

import java.util.Map;

import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectIdJsonDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Payload for the synthetic SSF event emitter admin endpoint.
 *
 * <p>Allows an IAM management client to push a single SSF event on behalf of
 * an upstream system that Keycloak can't observe natively (e.g. password
 * changes that happen in LDAP). The transmitter wraps the payload into a
 * signed SET and dispatches it through the normal delivery pipeline — the
 * receiver's subject-subscription and {@code events_delivered} filters still
 * apply.
 *
 * <p>The {@code sub_id} follows RFC 9493 (Subject Identifiers for Security
 * Event Tokens). Pick whichever format fits the upstream system's identity
 * model — {@code email}, {@code iss_sub}, {@code opaque} for simple user
 * subjects, or {@code complex} to nest {@code user}, {@code session},
 * {@code tenant} (for CAEP events that carry more than one identifier).
 * The transmitter passes the {@code sub_id} through verbatim, so the
 * receiver sees the exact format the emitter chose.
 *
 * <p>Example — credential change for a user identified by email:
 * <pre>
 * {
 *   "eventType": "CaepCredentialChange",
 *   "sub_id": { "format": "email", "email": "user@example.com" },
 *   "event":  { "credential_type": "password", "change_type": "update" }
 * }
 * </pre>
 *
 * <p>Example — session revoked with a complex subject (user + session):
 * <pre>
 * {
 *   "eventType": "CaepSessionRevoked",
 *   "sub_id": {
 *     "format":  "complex",
 *     "user":    { "format": "iss_sub", "iss": "https://kc.example.com/realms/foo", "sub": "user-uuid" },
 *     "session": { "format": "opaque",  "id":  "session-id" }
 *   },
 *   "event":  { "event_timestamp": 1713360000, "reason_admin": { "en": "..." } }
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SsfEmitEventRequest {

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("sub_id")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    private SubjectId subjectId;

    /**
     * Admin-shorthand alternative to {@link #subjectId}. Only honored
     * on the admin-emit path (caller has manage-clients on the
     * receiver) — the trusted-emitter path always uses {@code sub_id}
     * verbatim. Mirrors the {@code type}/{@code value} pair the admin
     * {@code /subjects:add} endpoints take:
     *
     * <ul>
     *     <li>{@code user-id}, {@code user-email}, {@code user-username}
     *         → resolves to a Keycloak user; transmitter builds the
     *         {@code sub_id} via
     *         {@link org.keycloak.ssf.transmitter.event.SecurityEventTokenMapper#buildSubjectForReceiver
     *         buildSubjectForReceiver} so the shape honors the receiver's
     *         configured {@code ssf.userSubjectFormat}.</li>
     *     <li>{@code org-alias} → resolves to an organization; transmitter
     *         emits a complex subject with a {@code tenant} facet only
     *         (no user) so the receiver routes it as an org-scoped
     *         event.</li>
     * </ul>
     *
     * <p>{@code sub_id} takes precedence if both shapes are present.
     */
    @JsonProperty("subjectType")
    private String subjectType;

    @JsonProperty("subjectValue")
    private String subjectValue;

    @JsonProperty("event")
    private Map<String, Object> event;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public SubjectId getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(SubjectId subjectId) {
        this.subjectId = subjectId;
    }

    public Map<String, Object> getEvent() {
        return event;
    }

    public void setEvent(Map<String, Object> event) {
        this.event = event;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getSubjectValue() {
        return subjectValue;
    }

    public void setSubjectValue(String subjectValue) {
        this.subjectValue = subjectValue;
    }
}
