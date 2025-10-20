package org.keycloak.protocol.ssf.stream;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.protocol.ssf.event.delivery.DeliveryMethod;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * See SET Token Delivery Using HTTP Profile https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-10.3.1.1
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class AbstractDeliveryMethodRepresentation {

    /**
     * Receiver-Supplied, REQUIRED. The specific delivery method to be used. This can be any one of "urn:ietf:rfc:8935" (push) or "urn:ietf:rfc:8936" (poll), but not both.
     */
    @JsonProperty("method")
    private final DeliveryMethod method;

    /**
     * endpoint_url
     * The URL where events are pushed through HTTP POST. This is set by the Receiver. If a Receiver is using multiple streams from a single Transmitter and needs to keep the SETs separated, it is RECOMMENDED that the URL for each stream be unique.
     */
    @JsonProperty("endpoint_url")
    private final URI endpointUrl;

    /**
     * authorization_header
     *
     * The HTTP Authorization header that the Transmitter MUST set with each event delivery, if the configuration is present. The value is optional, and it is set by the Receiver.
     */
    @JsonProperty("authorization_header")
    private String authorizationHeader;

    private Map<String, Object> metadata;

    protected AbstractDeliveryMethodRepresentation(DeliveryMethod method, URI endpointUrl) {
        this.method = method;
        this.endpointUrl = endpointUrl;
    }

    public DeliveryMethod getMethod() {
        return method;
    }

    public URI getEndpointUrl() {
        return endpointUrl;
    }

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    @JsonAnySetter
    public void setMetadataValue(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    public Object getMetadataValue(String key) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return this.metadata.get(key);
    }

    @JsonCreator
    public static AbstractDeliveryMethodRepresentation create(@JsonProperty("method") DeliveryMethod method, @JsonProperty("endpoint_url") URI endpointUrl, @JsonProperty("authorization_header") String authorizationHeader) {
        switch (method) {
            case PUSH:
                return new PushDeliveryMethodRepresentation(endpointUrl, authorizationHeader);
            case POLL:
                return new PollDeliveryMethodRepresentation(endpointUrl);
            default:
                throw new IllegalArgumentException();
        }
    }
}
