package org.keycloak.protocol.ssf.transmitter.stream;

import java.util.Set;

import org.keycloak.protocol.ssf.stream.StreamStatusValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a stream configuration in the SSF transmitter.
 *
 * See: https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamConfig {

    /**
     * Transmitter-Supplied, REQUIRED. A string that uniquely identifies the stream.
     * A Transmitter MUST generate a unique ID for each of its non-deleted streams at the time of stream creation.
     * Transmitters SHOULD use character set described in Section 2.3 of [RFC3986] to generate the stream ID
     */
    @JsonProperty("stream_id")
    protected String streamId;

    /**
     * Transmitter-Supplied, REQUIRED. A URL using the https scheme with no query or fragment component that the Transmitter asserts as its Issuer Identifier.
     * This MUST be identical to the "iss" Claim value in Security Event Tokens issued from this Transmitter.
     */
    @JsonProperty("iss")
    protected String issuer;

    /**
     * Transmitter-Supplied, REQUIRED. A string or an array of strings containing an audience claim as defined in JSON Web Token (JWT)[RFC7519] that identifies the Event Receiver(s) for the Event Stream.
     * This property cannot be updated. If multiple Receivers are specified then the Transmitter SHOULD know that these Receivers are the same entity.
     */
    @JsonProperty("aud")
    protected Set<String> audience;

    /**
     * Transmitter-Supplied, OPTIONAL. An array of URIs identifying the set of events supported by the Transmitter for this Receiver.
     * If omitted, Event Transmitters SHOULD make this set available to the Event Receiver via some other means (e.g. publishing it in online documentation).
     */
    @JsonProperty("events_supported")
    protected Set<String> eventsSupported;

    /**
     * Receiver-Supplied, OPTIONAL. An array of URIs identifying the set of events that the Receiver requested.
     * A Receiver SHOULD request only the events that it understands and it can act on.
     * This is configurable by the Receiver. A Transmitter MUST ignore any array values that it does not understand.
     * This array SHOULD NOT be empty.
     */
    @JsonProperty("events_requested")
    protected Set<String> eventsRequested;

    /**
     * Transmitter-Supplied, REQUIRED. An array of URIs identifying the set of events that the Transmitter MUST include in the stream.
     * This is a subset (not necessarily a proper subset) of the intersection of "events_supported" and "events_requested".
     * A Receiver MUST rely on the values received in this field to understand which event types it can expect from the Transmitter.
     */
    @JsonProperty("events_delivered")
    protected Set<String> eventsDelivered;

    /**
     * REQUIRED. A JSON object containing a set of name/value pairs specifying configuration parameters for the SET delivery method.
     * The actual delivery method is identified by the special key "method" with the value being a URI as defined in Section 6.1.
     */
    @JsonProperty("delivery")
    protected StreamDeliveryConfig delivery;

    /**
     * Transmitter-Supplied, OPTIONAL. An integer indicating the minimum amount of time in seconds that must pass in between verification requests.
     * If an Event Receiver submits verification requests more frequently than this, the Event Transmitter MAY respond with a 429 status code.
     * An Event Transmitter SHOULD NOT respond with a 429 status code if an Event Receiver is not exceeding this frequency.
     */
    @JsonProperty("min_verification_interval")
    protected Integer minVerificationInterval;

    /**
     * Receiver-Supplied, OPTIONAL. A string that describes the properties of the stream.
     * This is useful in multi-stream systems to identify the stream for human actors.
     * The transmitter MAY truncate the string beyond an allowed max length.
     */
    @JsonProperty("description")
    protected String description;

    /**
     * Transmitter-Supplied, OPTIONAL. The refreshable inactivity timeout of the stream in seconds.
     * After the timeout duration passes with no eligible activity from the Receiver, as defined below, the Transmitter MAY either pause, disable, or delete the stream.
     * The syntax is the same as that of expires_in from Section A.14 of [RFC6749].
     */
    @JsonProperty("inactivity_timeout")
    protected Integer inactivityTimeout;

    @JsonProperty("kc_status")
    protected StreamStatusValue status;

    @JsonProperty("kc_status_reason")
    protected String statusReason;

    @JsonIgnore
    protected Integer createdAt;

    @JsonIgnore
    protected Integer updatedAt;

    /**
     * The subject identifier format expected for any SET transmitted. Defaults to `iss_sub`.
     *
     * Non-standard format field needed for compatibility, e.g. for Apple Business Manager
     */
    @JsonProperty("format")
    protected String format = "iss_sub";

    @JsonIgnore
    protected String profile;

    @JsonProperty("kc_enabled")
    protected Boolean enabled;

    /**
     * Indicates how the verification is triggered.
     */
    @JsonProperty("kc_verification_trigger")
    protected VerificationTrigger verificationTrigger;

    /**
     * The verification delay in milliseconds, in case the transmitter triggers the verification.
     */
    @JsonProperty("kc_verification_delay_millis")
    protected Integer verificationDelayMillis;


    public enum VerificationTrigger {
        TRANSMITTER_INITIATED,
        RECEIVER_INITIATED
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
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

    public StreamDeliveryConfig getDelivery() {
        return delivery;
    }

    public void setDelivery(StreamDeliveryConfig delivery) {
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

    public Integer getInactivityTimeout() {
        return inactivityTimeout;
    }

    public void setInactivityTimeout(Integer inactivityTimeout) {
        this.inactivityTimeout = inactivityTimeout;
    }

    public StreamStatusValue getStatus() {
        return status;
    }

    public void setStatus(StreamStatusValue status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public VerificationTrigger getVerificationTrigger() {
        return verificationTrigger;
    }

    public void setVerificationTrigger(VerificationTrigger verificationTrigger) {
        this.verificationTrigger = verificationTrigger;
    }

    public Integer getVerificationDelayMillis() {
        return verificationDelayMillis;
    }

    public void setVerificationDelayMillis(Integer verificationDelayMillis) {
        this.verificationDelayMillis = verificationDelayMillis;
    }
}
