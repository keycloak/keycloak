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
public abstract class AbstractSetDeliveryMethodRepresentation {

    /**
     * Receiver-Supplied, REQUIRED. The specific delivery method to be used. This can be any one of "urn:ietf:rfc:8935" (push) or "urn:ietf:rfc:8936" (poll), but not both.
     */
    @JsonProperty("method")
    private final DeliveryMethod method;

    private Map<String, Object> metadata;

    protected AbstractSetDeliveryMethodRepresentation(DeliveryMethod method) {
        this.method = method;
    }

    public DeliveryMethod getMethod() {
        return method;
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
    public static AbstractSetDeliveryMethodRepresentation create(@JsonProperty("method") DeliveryMethod method, @JsonProperty("endpoint_url") String endpointUrl, @JsonProperty("authorization_header") String authHeader) {
        switch (method) {
            case PUSH:
                return new PushDeliveryMethodRepresentation(endpointUrl, authHeader);
            case POLL:
                return new PollSetDeliveryMethodRepresentation(endpointUrl);
            default:
                throw new IllegalArgumentException();
        }
    }
}
