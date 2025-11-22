package org.keycloak.protocol.ssf.event.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * See: https://datatracker.ietf.org/doc/html/rfc9493#section-3.2.7
 */
public class UriSubjectId extends SubjectId {

    public static final String TYPE = "uri";

    @JsonProperty("uri")
    protected String uri;

    public UriSubjectId() {
        super(TYPE);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return "UriSubjectId{" +
               "uri='" + uri + '\'' +
               '}';
    }
}
