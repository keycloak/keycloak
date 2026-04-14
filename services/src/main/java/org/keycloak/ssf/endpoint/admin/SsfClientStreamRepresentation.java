package org.keycloak.ssf.endpoint.admin;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Representation of the current SSF stream state for a single receiver client,
 * exposed via {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/clients/{clientId}/stream}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SsfClientStreamRepresentation {

    private String streamId;

    private Set<String> audience;

    private Set<String> eventsSupported;

    private Set<String> eventsRequested;

    private Set<String> eventsDelivered;

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
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
}
