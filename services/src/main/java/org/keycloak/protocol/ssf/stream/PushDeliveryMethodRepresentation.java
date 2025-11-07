package org.keycloak.protocol.ssf.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.protocol.ssf.event.DeliveryMethod;

import java.util.Objects;

/**
 * See: 10.3.1.1. Push Delivery using HTTP https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-10.3.1.1
 */
public class PushDeliveryMethodRepresentation extends AbstractSetDeliveryMethodRepresentation {

    /**
     * authorization_header
     *
     * The HTTP Authorization header that the Transmitter MUST set with each event delivery, if the configuration is present. The value is optional and it is set by the Receiver.
     */
    @JsonProperty("authorization_header")
    protected String authorizationHeader;

    /**
     * endpoint_url
     * The URL where events are pushed through HTTP POST. This is set by the Receiver. If a Receiver is using multiple streams from a single Transmitter and needs to keep the SETs separated, it is RECOMMENDED that the URL for each stream be unique.
     */
    @JsonProperty("endpoint_url")
    protected String endpointUrl;

    /**
     * @param endpointUrl MUST be supplied by the Receiver
     * @param authorizationHeader MAY be supploed by the Receiver
     */
    public PushDeliveryMethodRepresentation(String endpointUrl, String authorizationHeader) {
        super(DeliveryMethod.PUSH);
        this.endpointUrl = Objects.requireNonNull(endpointUrl, "endpointUrl");
        this.authorizationHeader = authorizationHeader;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = Objects.requireNonNull(endpointUrl, "endpointUrl");
    }

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }
}
