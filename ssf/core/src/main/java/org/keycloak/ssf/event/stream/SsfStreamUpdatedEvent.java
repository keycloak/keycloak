package org.keycloak.ssf.event.stream;

import java.util.Map;

import org.keycloak.ssf.stream.StreamStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SSF Stream status updated event.
 *
 * See: https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#name-stream-updated-event
 */
public class SsfStreamUpdatedEvent extends SsfStreamEvent {

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

    public SsfStreamUpdatedEvent() {
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

    @Override
    public Map<String, Object> createAdminDetails() {
        var adminRepresentation = super.createAdminDetails();
        adminRepresentation.put("status", status);
        if (reason != null) {
            adminRepresentation.put("reason", reason);
        }
        return adminRepresentation;
    }
}
