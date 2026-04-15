package org.keycloak.ssf.transmitter.admin;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Representation of the current SSF stream state for a single receiver client,
 * exposed via {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientId}/stream}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SsfClientStreamRepresentation {

    private String streamId;

    private String description;

    private String status;

    private String statusReason;

    private Set<String> audience;

    private Set<String> eventsSupported;

    private Set<String> eventsRequested;

    private Set<String> eventsDelivered;

    private Integer createdAt;

    private Integer updatedAt;

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Set<String> getAudience() {
        return audience;
    }

    public void setAudience(Set<String> audience) {
        this.audience = audience;
    }

    public Set<String> getEventsSupported() {
        return eventsSupported;
    }

    public void setEventsSupported(Set<String> eventsSupported) {
        this.eventsSupported = eventsSupported;
    }

    public Set<String> getEventsRequested() {
        return eventsRequested;
    }

    public void setEventsRequested(Set<String> eventsRequested) {
        this.eventsRequested = eventsRequested;
    }

    public Set<String> getEventsDelivered() {
        return eventsDelivered;
    }

    public void setEventsDelivered(Set<String> eventsDelivered) {
        this.eventsDelivered = eventsDelivered;
    }

    public Integer getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Integer createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Integer updatedAt) {
        this.updatedAt = updatedAt;
    }
}
