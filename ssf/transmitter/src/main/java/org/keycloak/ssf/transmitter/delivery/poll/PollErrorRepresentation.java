package org.keycloak.ssf.transmitter.delivery.poll;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error response body for the RFC 8936 poll endpoint. The poll spec
 * mandates the {@code err} / {@code description} key pair (§2.4.4),
 * which differs from the OAuth-style {@code error} / {@code error_description}
 * envelope the rest of the SSF transmitter uses via
 * {@link org.keycloak.ssf.transmitter.support.SsfErrorRepresentation
 * SsfErrorRepresentation}. This DTO carries the poll-specific shape so
 * a strict RFC 8936 receiver doesn't have to translate.
 *
 * <p>The same key pair is already used inside the request body's
 * {@code setErrs} map ({@link PollRequest}), so this representation
 * keeps the request and response error envelopes symmetric.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PollErrorRepresentation {

    @JsonProperty("err")
    private String err;

    @JsonProperty("description")
    private String description;

    public PollErrorRepresentation() {
    }

    public PollErrorRepresentation(String err, String description) {
        this.err = err;
        this.description = description;
    }

    public String getErr() {
        return err;
    }

    public void setErr(String err) {
        this.err = err;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
