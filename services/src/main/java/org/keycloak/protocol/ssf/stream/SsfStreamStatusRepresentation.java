package org.keycloak.protocol.ssf.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SsfStreamStatusRepresentation {

    @JsonProperty("stream_id")
    private String streamId;

    @JsonProperty("status")
    private StreamStatus status;

    @JsonProperty("reason")
    private String reason;

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public StreamStatus getStatus() {
        return status;
    }

    public void setStatus(StreamStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
