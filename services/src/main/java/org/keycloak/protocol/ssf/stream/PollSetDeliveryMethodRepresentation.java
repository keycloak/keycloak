package org.keycloak.protocol.ssf.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.protocol.ssf.event.DeliveryMethod;

import java.util.Objects;

public class PollSetDeliveryMethodRepresentation extends AbstractSetDeliveryMethodRepresentation {

    /**
     * endpoint_url
     * The URL where events can be retrieved from. This is specified by the Transmitter. These URLs MAY be reused across Receivers, but MUST be unique per stream for a given Receiver.
     */
    @JsonProperty("endpoint_url")
    protected String endpointUrl;

    public PollSetDeliveryMethodRepresentation(String endpointUrl) {
        super(DeliveryMethod.POLL);
        this.endpointUrl = endpointUrl;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = Objects.requireNonNull(endpointUrl, "endpointUrl");
    }
}
