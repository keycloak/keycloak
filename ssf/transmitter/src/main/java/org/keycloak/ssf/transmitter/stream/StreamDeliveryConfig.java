package org.keycloak.ssf.transmitter.stream;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the delivery configuration for a stream.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamDeliveryConfig {

    @JsonProperty("method")
    private String method;

    @JsonProperty("endpoint_url")
    private String endpointUrl;

    @JsonProperty("authorization_header")
    private String authorizationHeader;

    @JsonProperty("additional_parameters")
    private Map<String, Object> additionalParameters;

    public StreamDeliveryConfig() {
    }

    /**
     * Shallow-ish copy constructor used by {@link StreamConfig#StreamConfig(StreamConfig)}
     * so a draft delivery config can be mutated (e.g. by
     * {@code finalizePollEndpointUrlIfApplicable}) without touching the
     * stored instance.
     */
    public StreamDeliveryConfig(StreamDeliveryConfig other) {
        if (other == null) {
            return;
        }
        this.method = other.method;
        this.endpointUrl = other.endpointUrl;
        this.authorizationHeader = other.authorizationHeader;
        this.additionalParameters = other.additionalParameters == null
                ? null
                : new LinkedHashMap<>(other.additionalParameters);
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    public Map<String, Object> getAdditionalParameters() {
        return additionalParameters;
    }

    public void setAdditionalParameters(Map<String, Object> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }
}
