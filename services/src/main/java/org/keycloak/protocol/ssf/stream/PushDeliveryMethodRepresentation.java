package org.keycloak.protocol.ssf.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.protocol.ssf.event.delivery.DeliveryMethod;

import java.net.URI;
import java.util.Objects;

/**
 * See: 10.3.1.1. Push Delivery using HTTP https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-10.3.1.1
 */
public class PushDeliveryMethodRepresentation extends AbstractDeliveryMethodRepresentation {


    /**
     * authorization_header
     *
     * The HTTP Authorization header that the Transmitter MUST set with each event delivery, if the configuration is present. The value is optional and it is set by the Receiver.
     */
    @JsonProperty("authorization_header")
    protected String authorizationHeader;

    /**
     * @param endpointUrl MUST be supplied by the Receiver
     * @param authorizationHeader MAY be supploed by the Receiver
     */
    public PushDeliveryMethodRepresentation(URI endpointUrl, String authorizationHeader) {
        super(DeliveryMethod.PUSH, Objects.requireNonNull(endpointUrl, "endpointUrl"));
        this.authorizationHeader = authorizationHeader;
    }

    @Override
    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    @Override
    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }
}
