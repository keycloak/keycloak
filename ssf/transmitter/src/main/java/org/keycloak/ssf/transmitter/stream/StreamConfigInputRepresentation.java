package org.keycloak.ssf.transmitter.stream;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire-format DTO for the request body of {@code POST /streams} (SSF spec
 * §8.1.1.2). Carries the receiver-writable subset of a stream configuration
 * plus nothing else: the transmitter generates {@code stream_id}, {@code iss},
 * {@code aud}, {@code events_supported}, {@code events_delivered} and the
 * Keycloak {@code kc_*} extensions itself, so those fields are intentionally
 * absent from the input type. Jackson's default
 * {@code FAIL_ON_UNKNOWN_PROPERTIES} behaviour rejects any such field with
 * 400 at bind time.
 *
 * <p>{@link StreamConfigUpdateRepresentation} extends this class and adds
 * {@code stream_id} so PATCH/PUT requests can identify the existing stream
 * they target — create requests must not carry a receiver-supplied
 * {@code stream_id}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamConfigInputRepresentation {

    @JsonProperty("description")
    protected String description;

    @JsonProperty("events_requested")
    protected Set<String> eventsRequested;

    @JsonProperty("delivery")
    protected StreamDeliveryConfig delivery;

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

    public StreamDeliveryConfig getDelivery() {
        return delivery;
    }

    public void setDelivery(StreamDeliveryConfig delivery) {
        this.delivery = delivery;
    }
}
