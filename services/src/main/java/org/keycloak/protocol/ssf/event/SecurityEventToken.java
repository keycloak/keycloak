package org.keycloak.protocol.ssf.event;

import java.util.LinkedHashMap;
import java.util.Map;

import org.keycloak.protocol.ssf.event.subjects.SubjectId;
import org.keycloak.protocol.ssf.event.subjects.SubjectIdJsonDeserializer;
import org.keycloak.representations.JsonWebToken;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents a RFC8417 Security Event Token (SET).
 *
 * See: https://datatracker.ietf.org/doc/html/rfc8417
 */
public class SecurityEventToken extends JsonWebToken {

    @JsonProperty("sub_id")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    protected SubjectId subjectId;

    @JsonProperty("txn")
    protected String txn;

    @JsonProperty("events")
    protected Map<String, Map<String,Object>> events;

    public SecurityEventToken txn(String txn) {
        setTxn(txn);
        return this;
    }

    public SubjectId getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(SubjectId subjectId) {
        this.subjectId = subjectId;
    }

    public SecurityEventToken subjectId(SubjectId subjectId) {
        setSubjectId(subjectId);
        return this;
    }

    public Map<String, Map<String,Object>> getEvents() {
        if (events == null) {
            events = new LinkedHashMap<>();
        }
        return events;
    }

    public void setEvents(Map<String, Map<String,Object>> events) {
        this.events = events;
    }

    public String getTxn() {
        return txn;
    }

    public void setTxn(String txn) {
        this.txn = txn;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
