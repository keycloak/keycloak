package org.keycloak.ssf.transmitter.delivery.poll;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * RFC 8936 §2.2 polling response body.
 *
 * <p>{@code sets} maps each delivered SET's {@code jti} to its signed JWS
 * payload (the receiver acks by passing those jtis back in the next
 * request's {@code ack} array). {@code moreAvailable} signals whether
 * the receiver should poll again immediately to drain remaining
 * pending events.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PollResponse {

    @JsonProperty("sets")
    private Map<String, String> sets = new LinkedHashMap<>();

    @JsonProperty("moreAvailable")
    private boolean moreAvailable;

    public Map<String, String> getSets() {
        return sets;
    }

    public void setSets(Map<String, String> sets) {
        this.sets = sets;
    }

    public boolean isMoreAvailable() {
        return moreAvailable;
    }

    public void setMoreAvailable(boolean moreAvailable) {
        this.moreAvailable = moreAvailable;
    }
}
