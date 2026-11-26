package org.keycloak.ssf.transmitter.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a verification request in the SSF transmitter.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamVerificationRequest {

    @JsonProperty("stream_id")
    private String streamId;

    @JsonProperty("state")
    private String state;

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
}
