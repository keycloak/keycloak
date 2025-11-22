package org.keycloak.protocol.ssf.event.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * See: https://datatracker.ietf.org/doc/html/rfc9493#name-decentralized-identifier-di
 */
public class DidSubjectId extends SubjectId {

    public static final String DID = "did";

    @JsonProperty("url")
    protected String url;

    public DidSubjectId() {
        super(DID);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "DidSubjectId{" +
               "url='" + url + '\'' +
               '}';
    }
}
