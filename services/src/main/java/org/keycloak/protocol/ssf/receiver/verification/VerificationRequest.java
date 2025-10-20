package org.keycloak.protocol.ssf.receiver.verification;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerificationRequest {

    @JsonProperty("stream_id")
    protected String streamId;

    @JsonProperty("state")
    protected String state;

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "VerificationRequest{" +
               "streamId='" + streamId + '\'' +
               ", state='" + state + '\'' +
               '}';
    }
}
