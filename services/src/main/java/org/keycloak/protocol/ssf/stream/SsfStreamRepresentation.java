package org.keycloak.protocol.ssf.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.net.URI;
import java.util.List;

/**
 * See: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#name-stream-configuration
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"iss", "aud", "events_supported", "events_requested", "events_delivered", "delivery", "min_verification_interval", "format"})
public class SsfStreamRepresentation {

    //see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-7.1.1

    /**
     * Transmitter-Supplied, REQUIRED. A string that uniquely identifies the stream. A Transmitter MUST generate a unique ID for each of its non-deleted streams at the time of stream creation.
     */
    @JsonProperty("stream_id")
    private String id;

    /**
     * Receiver-Supplied, OPTIONAL. A string that describes the properties of the stream. This is useful in multi-stream systems to identify the stream for human actors. The transmitter MAY truncate the string beyond an allowed max length.
     */
    @JsonProperty("description")
    private String description;

    /**
     * Transmitter-Supplied, REQUIRED. A URL using the https scheme with no query or fragment component that the Transmitter asserts as its Issuer Identifier. This MUST be identical to the "iss" Claim value in Security Event Tokens issued from this Transmitter.
     */
    @JsonProperty("iss")
    private URI issuer;

    /**
     * Transmitter-Supplied, REQUIRED. A string or an array of strings containing an audience claim as defined in JSON Web Token (JWT)[RFC7519] that identifies the Event Receiver(s) for the Event Stream. This property cannot be updated. If multiple Receivers are specified then the Transmitter SHOULD know that these Receivers are the same entity.
     */
    @JsonProperty("aud")
    private Object audience; // Can be URI or List<URI>

    /**
     * Transmitter-Supplied, OPTIONAL. An array of URIs identifying the set of events supported by the Transmitter for this Receiver. If omitted, Event Transmitters SHOULD make this set available to the Event Receiver via some other means (e.g. publishing it in online documentation).
     */
    @JsonProperty("events_supported")
    private List<URI> eventsSupported;

    /**
     * Receiver-Supplied, OPTIONAL. An array of URIs identifying the set of events that the Receiver requested. A Receiver SHOULD request only the events that it understands and it can act on. This is configurable by the Receiver. A Transmitter MUST ignore any array values that it does not understand. This array SHOULD NOT be empty.
     */
    @JsonProperty("events_requested")
    private List<URI> eventsRequested;

    /**
     * Transmitter-Supplied, REQUIRED. An array of URIs identifying the set of events that the Transmitter MUST include in the stream. This is a subset (not necessarily a proper subset) of the intersection of "events_supported" and "events_requested". A Receiver MUST rely on the values received in this field to understand which event types it can expect from the Transmitter.
     */
    @JsonProperty("events_delivered")
    private List<URI> eventsDelivered;

    /**
     * REQUIRED. A JSON object containing a set of name/value pairs specifying configuration parameters for the SET delivery method. The actual delivery method is identified by the special key "method" with the value being a URI as defined in Section 10.3.1. The value of the "delivery" field contains two sub-fields:
     */
    @JsonProperty("delivery")
    private AbstractDeliveryMethodRepresentation delivery;

    /**
     * Transmitter-Supplied, OPTIONAL. An integer indicating the minimum amount of time in seconds that must pass in between verification requests. If an Event Receiver submits verification requests more frequently than this, the Event Transmitter MAY respond with a 429 status code. An Event Transmitter SHOULD NOT respond with a 429 status code if an Event Receiver is not exceeding this frequency.
     */
    @JsonProperty("min_verification_interval")
    private Integer minVerificationInterval;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public URI getIssuer() {
        return issuer;
    }

    public void setIssuer(URI issuer) {
        this.issuer = issuer;
    }

    public Object getAudience() {
        return audience;
    }

    public void setAudience(Object audience) {
        this.audience = audience;
    }

    public List<URI> getEventsSupported() {
        return eventsSupported;
    }

    public void setEventsSupported(List<URI> eventsSupported) {
        this.eventsSupported = eventsSupported;
    }

    public List<URI> getEventsRequested() {
        return eventsRequested;
    }

    public void setEventsRequested(List<URI> eventsRequested) {
        this.eventsRequested = eventsRequested;
    }

    public List<URI> getEventsDelivered() {
        return eventsDelivered;
    }

    public void setEventsDelivered(List<URI> eventsDelivered) {
        this.eventsDelivered = eventsDelivered;
    }

    public AbstractDeliveryMethodRepresentation getDelivery() {
        return delivery;
    }

    public void setDelivery(AbstractDeliveryMethodRepresentation delivery) {
        this.delivery = delivery;
    }

    public Integer getMinVerificationInterval() {
        return minVerificationInterval;
    }

    public void setMinVerificationInterval(Integer minVerificationInterval) {
        this.minVerificationInterval = minVerificationInterval;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
