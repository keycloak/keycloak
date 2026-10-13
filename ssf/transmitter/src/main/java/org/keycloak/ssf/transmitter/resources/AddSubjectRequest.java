package org.keycloak.ssf.transmitter.resources;

import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectIdJsonDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddSubjectRequest {

    @JsonProperty("stream_id")
    private String streamId;

    @JsonProperty("subject")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    private SubjectId subject;

    @JsonProperty("verified")
    private Boolean verified;

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public SubjectId getSubject() {
        return subject;
    }

    public void setSubject(SubjectId subject) {
        this.subject = subject;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }
}
