package org.keycloak.protocol.ssf.event.types.stream;

import org.keycloak.protocol.ssf.stream.StreamStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SSF Stream status updated event.
 *
 * See: https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#name-stream-updated-event
 */
public class StreamUpdatedEvent extends StreamEvent {

    public static final String TYPE = "https://schemas.openid.net/secevent/ssf/event-type/stream-updated";

    /**
     * REQUIRED. Defines the new status of the stream.
     */
    @JsonProperty("status")
    protected StreamStatus status;

    /**
     * OPTIONAL. Provides a short description of why the Transmitter has updated the status.
     */
    @JsonProperty("reason")
    protected String reason;

    public StreamUpdatedEvent() {
        super(TYPE);
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
