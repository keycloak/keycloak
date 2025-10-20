package org.keycloak.protocol.ssf.event.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * See: https://datatracker.ietf.org/doc/html/rfc9493#name-opaque-identifier-format
 */
public class OpaqueSubjectId extends SubjectId {

    public static final String TYPE = "opaque";

    @JsonProperty("id")
    protected String id;

    public OpaqueSubjectId() {
        super(TYPE);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "OpaqueSubjectId{" +
               "id='" + id + '\'' +
               '}';
    }
}
