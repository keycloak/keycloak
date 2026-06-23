package org.keycloak.ssf.transmitter.admin;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body of {@code PATCH /admin/realms/{realm}/ssf/clients/{clientId}/stream}.
 * Carries the admin-editable subset of a stream configuration; any
 * subset of fields may be supplied. Fields that aren't present (or are
 * explicitly null) leave the corresponding stored value untouched.
 *
 * <p>Receiver-supplied fields like {@code aud}, {@code iss},
 * {@code delivery}, {@code default_subjects} and the per-receiver
 * client attributes ({@code ssf.streamAudience},
 * {@code ssf.userSubjectFormat}, …) are deliberately not part of this
 * request — those are configured on the receiver client itself, not on
 * the stream.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SsfAdminStreamUpdateRequest {

    @JsonProperty("description")
    private String description;

    @JsonProperty("events_requested")
    private Set<String> eventsRequested;

    /**
     * Honoured verbatim when supplied — admin-supplied
     * {@code events_delivered} overrides the
     * {@code events_requested ∩ events_supported} intersection that
     * the receiver-facing endpoint computes. When omitted, the
     * intersection is recomputed from whatever
     * {@code events_requested} ends up being (either the new value if
     * supplied, or the existing stored value).
     */
    @JsonProperty("events_delivered")
    private Set<String> eventsDelivered;

    public SsfAdminStreamUpdateRequest() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
