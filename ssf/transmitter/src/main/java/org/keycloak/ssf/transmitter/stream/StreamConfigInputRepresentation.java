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
 *
 * @see https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-8.1.1
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamConfigInputRepresentation {

    @JsonProperty("description")
    protected String description;

    @JsonProperty("events_requested")
    protected Set<String> eventsRequested;

    @JsonProperty("delivery")
    protected StreamDeliveryConfig delivery;

    // ------------------------------------------------------------------
    //  Legacy compatibility fields for SSE CAEP 1.0 ID1
    // See: https://openid.net/specs/openid-sse-framework-1_0.html#rfc.section.7.1.2
    //
    //  Per SSF §8.1.1.1 these are transmitter-supplied — a spec-compliant
    //  receiver MUST NOT include them in a create/update body. Apple
    //  Business Manager's legacy CAEP SSE profile nevertheless echoes them
    //  back in its create request (it round-trips the GET metadata shape
    //  and submits it as a create), so we declare them here purely so
    //  Jackson's default FAIL_ON_UNKNOWN_PROPERTIES doesn't 400 those
    //  requests at bind time.
    //
    //  Behavioural contract:
    //    * {@code iss}  — always ignored. The transmitter unconditionally
    //                     sets the issuer from its own metadata.
    //    * {@code aud}  — used as a fallback when the receiver client
    //                     does not have an admin-configured
    //                     {@code ssf.streamAudience} attribute. This lets
    //                     Apple's federation feed URL land on the stored
    //                     stream instead of being overwritten by the
    //                     generated {@code clientId/streamId} default.
    //                     Admin-configured audience still wins.
    //    * {@code format} — always ignored. The per-stream subject format
    //                     is driven by the {@code ssf.userSubjectFormat}
    //                     client attribute, validated against
    //                     {@code SsfUserSubjectFormats.ALLOWED}.
    // ------------------------------------------------------------------

    // LEGACY FIELDS START
    @JsonProperty("iss")
    protected String issuer;

    @JsonProperty("aud")
    protected Set<String> audience;

    @JsonProperty("format")
    protected String format;
    // LEGACY FIELDS END

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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
