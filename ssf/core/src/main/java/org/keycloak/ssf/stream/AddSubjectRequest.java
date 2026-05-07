package org.keycloak.ssf.stream;

import org.keycloak.ssf.subject.SubjectId;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddSubjectRequest {

    /**
     * REQUIRED. A string identifying the stream to which the subject is being added.
     */
    @JsonProperty("stream_id")
    private String streamId;

    /**
     * REQUIRED. A Subject claim identifying the subject to be added.
     */
    @JsonProperty("subject")
    private SubjectId subject;

    /**
     * OPTIONAL. A boolean value; when true, it indicates that the Event Receiver has verified the Subject claim. When false, it indicates that the Event Receiver has not verified the Subject claim. If omitted, Event Transmitters SHOULD assume that the subject has been verified.
     */
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
