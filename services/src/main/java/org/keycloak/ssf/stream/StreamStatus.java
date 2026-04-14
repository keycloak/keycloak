package org.keycloak.ssf.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a stream status in the SSF transmitter.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamStatus {

    @JsonProperty("stream_id")
    private String streamId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("reason")
    private String reason;

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
