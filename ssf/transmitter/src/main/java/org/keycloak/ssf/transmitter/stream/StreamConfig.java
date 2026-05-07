package org.keycloak.ssf.transmitter.stream;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.ssf.SsfProfile;
import org.keycloak.ssf.metadata.DefaultSubjects;
import org.keycloak.ssf.stream.StreamStatusValue;
import org.keycloak.ssf.subject.IssuerSubjectId;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.event.SsfSignatureAlgorithms;
import org.keycloak.ssf.transmitter.event.SsfUserSubjectFormats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a stream configuration in the SSF transmitter.
 *
 * See: https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
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

    /**
     * Receiver-Supplied, OPTIONAL. Controls whether the transmitter
     * delivers events for all subjects or only for explicitly subscribed
     * ones. {@code null} inherits the transmitter-wide default (usually
     * {@link DefaultSubjects#ALL}).
     */
    @JsonProperty("default_subjects")
    protected DefaultSubjects defaultSubjects;

    /**
     * Stream status. Tracked internally and surfaced to receivers via the
     * dedicated SSF {@code /streams/status} endpoint (SSF §8.1.5) — never
     * serialized as part of the {@code StreamConfig} wire shape returned
     * from {@code POST/GET/PATCH/PUT /streams}. {@code WRITE_ONLY} so
     * pre-refactor blobs that still carry {@code kc_status} are read
     * back during the legacy storage migration.
     */
    @JsonProperty(value = "kc_status", access = JsonProperty.Access.WRITE_ONLY)
    protected StreamStatusValue status;

    /**
     * Free-text status reason. Admin-only — the admin UI surfaces this via
     * {@link org.keycloak.ssf.transmitter.admin.SsfClientStreamRepresentation};
     * receivers see it through {@code /streams/status}. Same {@code WRITE_ONLY}
     * pattern as {@link #status} so legacy blobs still migrate cleanly.
     */
    @JsonProperty(value = "kc_status_reason", access = JsonProperty.Access.WRITE_ONLY)
    protected String statusReason;

    /**
     * Stream creation timestamp (epoch seconds). Admin/audit field, surfaced
     * to operators via the admin UI — not part of the SSF spec and never
     * leaked on the receiver-facing wire. {@code WRITE_ONLY} so legacy
     * blobs still hydrate this on read.
     */
    @JsonProperty(value = "kc_created_at", access = JsonProperty.Access.WRITE_ONLY)
    protected Integer createdAt;

    /**
     * Last update timestamp (epoch seconds). Same admin/audit role as
     * {@link #createdAt}; same {@code WRITE_ONLY} treatment.
     */
    @JsonProperty(value = "kc_updated_at", access = JsonProperty.Access.WRITE_ONLY)
    protected Integer updatedAt;

    /**
     * Subject identifier format. Not part of SSF 1.0 §8.1.1 — only valid
     * on streams whose receiver client uses the legacy
     * {@link SsfProfile#SSE_CAEP SSE_CAEP} profile (Apple Business
     * Manager / Apple School Manager). Persisted and echoed back in wire
     * responses so CAEP receivers can round-trip their stream
     * representation. For SSF 1.0 receivers this field stays {@code null}
     * and never appears on the wire thanks to {@code @JsonInclude(NON_NULL)}.
     */
    @JsonProperty("format")
    protected String format;

    @JsonIgnore
    protected SsfProfile profile;

    @JsonIgnore
    protected Boolean enabled;

    /**
     * The receiver client's Keycloak id this stream is registered
     * against. Populated at
     * {@link org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore#extractStreamConfig
     * extractStreamConfig} time so the dispatcher / outbox can
     * identify the owning client without a second lookup. In
     * particular, the push outbox row needs it to re-resolve the
     * receiver on retries. Never serialized to the wire.
     */
    @JsonIgnore
    protected String clientId;

    /**
     * The receiver client's human-readable OAuth {@code client_id}
     * (e.g. {@code AppleBusinessManagerOIDC}). Used as the key suffix
     * in {@code ssf.notify.<clientClientId>} user/org attributes so
     * the attribute is readable in the admin UI. Populated from
     * {@link org.keycloak.models.ClientModel#getClientId()} at
     * {@code extractStreamConfig} time.
     */
    @JsonIgnore
    protected String clientClientId;

    @JsonIgnore
    protected Integer pushEndpointConnectTimeoutMillis;

    @JsonIgnore
    protected Integer pushEndpointSocketTimeoutMillis;

    /**
     * Per-receiver override of the JWS signature algorithm used to sign
     * SSF Security Event Tokens delivered to this stream. Populated from
     * the receiver client's {@code ssf.signatureAlgorithm} attribute when
     * the stream is loaded; {@code null} means "fall through to the
     * transmitter-wide default from {@link SsfTransmitterConfig}".
     * Validated against {@link SsfSignatureAlgorithms#ALLOWED}
     * at stream create/update time.
     */
    @JsonIgnore
    protected String signatureAlgorithm;

    /**
     * Per-receiver selection of the subject identifier format the
     * transmitter should use for the <em>user</em> part of SSF Security
     * Event Tokens delivered to this stream. Populated from the receiver
     * client's {@code ssf.userSubjectFormat} attribute when the stream is
     * loaded; {@code null} means "fall through to the
     * {@link IssuerSubjectId#TYPE iss_sub}
     * default". Validated against
     * {@link SsfUserSubjectFormats#ALLOWED}
     * at stream create/update time.
     */
    @JsonIgnore
    protected String userSubjectFormat;

    /**
     * Per-receiver override of the SSF §9.3 subject-removal grace
     * window (seconds). Populated from the receiver client's
     * {@code ssf.subjectRemovalGraceSeconds} attribute when the
     * stream is loaded; {@code null} means "fall through to the
     * transmitter-wide default from
     * {@link org.keycloak.ssf.transmitter.SsfTransmitterConfig#getSubjectRemovalGraceSeconds()
     * SsfTransmitterConfig}". {@code 0} explicitly opts this receiver
     * out of the grace window even when the transmitter default is
     * positive.
     */
    @JsonIgnore
    protected Integer subjectRemovalGraceSeconds;

    /**
     * Subset of {@link #eventsSupported} that the native Keycloak event
     * listener will <em>not</em> auto-emit for this receiver — those
     * events still ship over the wire when explicitly fired through
     * the synthetic-emit endpoint, but Keycloak's automatic mapping
     * skips them. Stores resolved event-type URIs (canonical form),
     * not aliases. Empty/null = current default behaviour (every
     * supported event auto-emits). Populated from the receiver
     * client's {@code ssf.emitOnlyEvents} attribute.
     */
    @JsonIgnore
    protected Set<String> emitOnlyEvents;

    public StreamConfig() {
    }

    /**
     * Deep-ish copy constructor. Creates a draft snapshot that callers
     * can mutate (merge receiver fields, re-derive poll endpoint URL,
     * recompute {@code events_delivered}) without touching the stored
     * instance. A validation failure on the draft leaves the stored
     * config untouched. Mutable collections ({@code audience},
     * {@code eventsSupported}, {@code eventsRequested},
     * {@code eventsDelivered}) and the {@code delivery} sub-object are
     * defensively copied; primitives and immutable strings are shared.
     */
    public StreamConfig(StreamConfig other) {
        if (other == null) {
            return;
        }
        this.streamId = other.streamId;
        this.issuer = other.issuer;
        this.audience = other.audience == null ? null : new HashSet<>(other.audience);
        this.eventsSupported = other.eventsSupported == null ? null : new HashSet<>(other.eventsSupported);
        this.eventsRequested = other.eventsRequested == null ? null : new HashSet<>(other.eventsRequested);
        this.eventsDelivered = other.eventsDelivered == null ? null : new HashSet<>(other.eventsDelivered);
        this.delivery = other.delivery == null ? null : new StreamDeliveryConfig(other.delivery);
        this.minVerificationInterval = other.minVerificationInterval;
        this.description = other.description;
        this.inactivityTimeout = other.inactivityTimeout;
        this.defaultSubjects = other.defaultSubjects;
        this.status = other.status;
        this.statusReason = other.statusReason;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
        this.format = other.format;
        this.profile = other.profile;
        this.enabled = other.enabled;
        this.clientId = other.clientId;
        this.clientClientId = other.clientClientId;
        this.pushEndpointConnectTimeoutMillis = other.pushEndpointConnectTimeoutMillis;
        this.pushEndpointSocketTimeoutMillis = other.pushEndpointSocketTimeoutMillis;
        this.signatureAlgorithm = other.signatureAlgorithm;
        this.userSubjectFormat = other.userSubjectFormat;
        this.subjectRemovalGraceSeconds = other.subjectRemovalGraceSeconds;
        this.emitOnlyEvents = other.emitOnlyEvents == null ? null : new HashSet<>(other.emitOnlyEvents);
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

    public DefaultSubjects getDefaultSubjects() {
        return defaultSubjects;
    }

    public void setDefaultSubjects(DefaultSubjects defaultSubjects) {
        this.defaultSubjects = defaultSubjects;
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

    public void setProfile(SsfProfile profile) {
        this.profile = profile;
    }

    public SsfProfile getProfile() {
        return profile;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientClientId() {
        return clientClientId;
    }

    public void setClientClientId(String clientClientId) {
        this.clientClientId = clientClientId;
    }

    public Integer getPushEndpointConnectTimeoutMillis() {
        return pushEndpointConnectTimeoutMillis;
    }

    public void setPushEndpointConnectTimeoutMillis(Integer pushEndpointConnectTimeoutMillis) {
        this.pushEndpointConnectTimeoutMillis = pushEndpointConnectTimeoutMillis;
    }

    public Integer getPushEndpointSocketTimeoutMillis() {
        return pushEndpointSocketTimeoutMillis;
    }

    public void setPushEndpointSocketTimeoutMillis(Integer pushEndpointSocketTimeoutMillis) {
        this.pushEndpointSocketTimeoutMillis = pushEndpointSocketTimeoutMillis;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getUserSubjectFormat() {
        return userSubjectFormat;
    }

    public void setUserSubjectFormat(String userSubjectFormat) {
        this.userSubjectFormat = userSubjectFormat;
    }

    public Integer getSubjectRemovalGraceSeconds() {
        return subjectRemovalGraceSeconds;
    }

    public void setSubjectRemovalGraceSeconds(Integer subjectRemovalGraceSeconds) {
        this.subjectRemovalGraceSeconds = subjectRemovalGraceSeconds;
    }

    public Set<String> getEmitOnlyEvents() {
        return emitOnlyEvents;
    }

    public void setEmitOnlyEvents(Set<String> emitOnlyEvents) {
        this.emitOnlyEvents = emitOnlyEvents;
    }
}
