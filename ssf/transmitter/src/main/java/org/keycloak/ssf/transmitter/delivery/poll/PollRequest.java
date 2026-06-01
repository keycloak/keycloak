package org.keycloak.ssf.transmitter.delivery.poll;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * RFC 8936 §2.1 polling request body.
 *
 * <p>All fields are optional; defaults applied by {@link PollDeliveryService} are
 * {@code maxEvents=100}, {@code returnImmediately=true}, {@code ack=[]},
 * {@code setErrs={}}.
 *
 * <p>{@code returnImmediately} is parsed for forward compatibility but
 * always honoured as immediate in v1 — long polling is deferred. See
 * {@code keycloak-notes/ssf/design/ssf-poll-delivery.md} decision §5.
 *
 * <p>{@code setErrs} is the receiver's NACK channel: each entry maps the
 * jti of a SET the receiver received but couldn't process to an error
 * descriptor (RFC 8936 §2.1):
 * <pre>{@code
 * {
 *   "setErrs": {
 *     "<jti>": { "err": "invalid_issuer", "description": "..." }
 *   }
 * }
 * }</pre>
 * Matching outbox rows transition to
 * {@link org.keycloak.models.jpa.entities.OutboxEntryStatus#DEAD_LETTER DEAD_LETTER}
 * with the receiver-supplied error message in {@code last_error}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollRequest {

    @JsonProperty("maxEvents")
    private Integer maxEvents;

    @JsonProperty("returnImmediately")
    private Boolean returnImmediately;

    @JsonProperty("ack")
    private List<String> ack;

    @JsonProperty("setErrs")
    private Map<String, Map<String, Object>> setErrs;

    public Integer getMaxEvents() {
        return maxEvents;
    }

    public void setMaxEvents(Integer maxEvents) {
        this.maxEvents = maxEvents;
    }

    public Boolean getReturnImmediately() {
        return returnImmediately;
    }

    public void setReturnImmediately(Boolean returnImmediately) {
        this.returnImmediately = returnImmediately;
    }

    public List<String> getAck() {
        return ack;
    }

    public void setAck(List<String> ack) {
        this.ack = ack;
    }

    public Map<String, Map<String, Object>> getSetErrs() {
        return setErrs;
    }

    public void setSetErrs(Map<String, Map<String, Object>> setErrs) {
        this.setErrs = setErrs;
    }
}
